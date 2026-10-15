package com.preppal.support;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;

/**
 * 纯单元测试（无 Spring 上下文）中初始化 MyBatis-Plus 的 TableInfo 缓存。
 *
 * 不初始化的话，LambdaQueryWrapper 在解析实体方法引用时会抛
 * 「can not find lambda cache for this entity」。生产环境由 MyBatis 注册 Mapper 时自动完成，
 * 这里显式注册，避免测试之间产生「必须按某个顺序执行才能通过」的隐藏依赖。
 */
public final class MybatisPlusTestSupport {

    private MybatisPlusTestSupport() {
    }

    public static void registerEntities(Class<?>... entities) {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        for (Class<?> entity : entities) {
            TableInfoHelper.initTableInfo(assistant, entity);
        }
    }
}
