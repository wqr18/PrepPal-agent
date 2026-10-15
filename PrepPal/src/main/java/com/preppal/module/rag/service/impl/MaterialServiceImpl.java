package com.preppal.module.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.preppal.common.BusinessException;
import com.preppal.common.ResultCode;
import com.preppal.common.UserContext;
import com.preppal.common.VectorCodec;
import com.preppal.module.ai.EmbeddingClient;
import com.preppal.module.course.entity.Course;
import com.preppal.module.course.mapper.CourseMapper;
import com.preppal.module.rag.dto.MaterialVO;
import com.preppal.module.rag.entity.Material;
import com.preppal.module.rag.entity.MaterialChunk;
import com.preppal.module.rag.mapper.MaterialChunkMapper;
import com.preppal.module.rag.mapper.MaterialMapper;
import com.preppal.module.rag.service.DocumentParser;
import com.preppal.module.rag.service.MaterialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(List.of("txt", "md"));
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final int ERROR_MESSAGE_MAX_LENGTH = 1000;

    private final MaterialMapper materialMapper;
    private final MaterialChunkMapper materialChunkMapper;
    private final CourseMapper courseMapper;
    private final DocumentParser documentParser;
    private final EmbeddingClient embeddingClient;
    private final TransactionTemplate transactionTemplate;

    @Override
    public List<MaterialVO> list(Long courseId) {
        Long userId = UserContext.require();
        requireOwnedCourse(courseId, userId);
        return materialMapper.selectList(new LambdaQueryWrapper<Material>()
                        .eq(Material::getUserId, userId)
                        .eq(Material::getCourseId, courseId)
                        .orderByDesc(Material::getCreatedAt)
                        .orderByDesc(Material::getId))
                .stream()
                .map(MaterialVO::from)
                .collect(Collectors.toList());
    }

    @Override
    public MaterialVO upload(MultipartFile file, Long courseId) {
        Long userId = UserContext.require();
        requireOwnedCourse(courseId, userId);

        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择要上传的文件");
        }
        String filename = sanitizeFilename(file.getOriginalFilename());
        String extension = extensionOf(filename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ResultCode.UNSUPPORTED_FILE);
        }
        long fileSize = file.getSize();
        if (fileSize > MAX_FILE_SIZE) {
            throw new BusinessException(ResultCode.FILE_TOO_LARGE);
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "文件读取失败，请重试");
        }

        // 编码识别与长度校验失败：记录一条 FAILED 资料，让前端展示失败原因
        String content;
        try {
            content = documentParser.decode(bytes);
            documentParser.validateLength(content);
        } catch (BusinessException e) {
            return saveFailed(userId, courseId, filename, fileSize, extension, e.getMessage());
        }

        // 同课程内容重复：直接复用已有记录，不重复切片与调用嵌入模型
        String contentHash = sha256(content);
        Material existing = findDuplicate(userId, courseId, contentHash);
        if (existing != null) {
            log.info("资料内容重复，复用已有记录 id={}", existing.getId());
            return MaterialVO.from(existing, true);
        }

        List<String> chunks;
        List<float[]> vectors;
        try {
            chunks = documentParser.chunk(content);
            if (chunks.isEmpty()) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR, "未从文件中解析出有效内容");
            }
            vectors = embeddingClient.embed(chunks);
            if (vectors.size() != chunks.size()) {
                throw new BusinessException(ResultCode.AI_ERROR, "向量生成数量与分片数不一致");
            }
        } catch (Exception e) {
            log.warn("资料解析或向量化失败 filename={}", filename, e);
            return saveFailed(userId, courseId, filename, fileSize, extension, rootMessage(e));
        }

        Long materialId = transactionTemplate.execute(status -> {
            Material material = new Material();
            material.setUserId(userId);
            material.setCourseId(courseId);
            material.setFilename(filename);
            material.setFileSize(fileSize);
            material.setMimeType(mimeTypeOf(extension));
            material.setContent(content);
            material.setContentHash(contentHash);
            material.setParseStatus(Material.STATUS_SUCCESS);
            materialMapper.insert(material);

            for (int i = 0; i < chunks.size(); i++) {
                MaterialChunk chunk = new MaterialChunk();
                chunk.setMaterialId(material.getId());
                chunk.setUserId(userId);
                chunk.setChunkIndex(i);
                chunk.setContent(chunks.get(i));
                chunk.setVector(VectorCodec.toJson(vectors.get(i)));
                materialChunkMapper.insert(chunk);
            }
            return material.getId();
        });

        touchCourse(courseId);
        log.info("资料解析完成 id={}, 分片数={}", materialId, chunks.size());
        return MaterialVO.from(materialMapper.selectById(materialId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long userId = UserContext.require();
        Material material = materialMapper.selectById(id);
        if (material == null || !userId.equals(material.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "资料不存在");
        }
        materialChunkMapper.delete(new LambdaQueryWrapper<MaterialChunk>()
                .eq(MaterialChunk::getMaterialId, id));
        materialMapper.deleteById(id);
    }

    private Material findDuplicate(Long userId, Long courseId, String contentHash) {
        return materialMapper.selectOne(new LambdaQueryWrapper<Material>()
                .eq(Material::getUserId, userId)
                .eq(Material::getCourseId, courseId)
                .eq(Material::getContentHash, contentHash)
                .eq(Material::getParseStatus, Material.STATUS_SUCCESS)
                .last("LIMIT 1"));
    }

    /** 失败记录单独写入（不参与事务回滚），保证失败原因可被前端看到 */
    private MaterialVO saveFailed(Long userId, Long courseId, String filename, long fileSize,
                                  String extension, String reason) {
        Material material = new Material();
        material.setUserId(userId);
        material.setCourseId(courseId);
        material.setFilename(filename);
        material.setFileSize(fileSize);
        material.setMimeType(mimeTypeOf(extension));
        material.setParseStatus(Material.STATUS_FAILED);
        material.setErrorMessage(truncate(reason, ERROR_MESSAGE_MAX_LENGTH));
        materialMapper.insert(material);
        return MaterialVO.from(materialMapper.selectById(material.getId()));
    }

    private void requireOwnedCourse(Long courseId, Long userId) {
        if (courseId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "缺少 courseId");
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null || !userId.equals(course.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课程不存在");
        }
    }

    private void touchCourse(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course != null) {
            course.setUpdatedAt(LocalDateTime.now());
            courseMapper.updateById(course);
        }
    }

    private String sanitizeFilename(String original) {
        if (original == null || original.isBlank()) {
            return "未命名文件";
        }
        String name = original.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.trim();
        if (name.isEmpty()) {
            return "未命名文件";
        }
        return name.length() > 256 ? name.substring(name.length() - 256) : name;
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String mimeTypeOf(String extension) {
        return "md".equals(extension) ? "text/markdown" : "text/plain";
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(Character.forDigit((b >> 4) & 0xF, 16));
                builder.append(Character.forDigit(b & 0xF, 16));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "计算资料指纹失败");
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return (message == null || message.isBlank()) ? "解析失败，请检查文件内容后重试" : message;
    }
}
