package com.liuliu.example.myblogbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liuliu.example.myblogbackend.dto.TagVO;
import com.liuliu.example.myblogbackend.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    /** 标签云：每个标签下【已发布】文章数，只返回还有已发布文章的标签 */
    @Select("SELECT t.id AS id, t.name AS name, COUNT(at2.article_id) AS article_count "
            + "FROM tag t "
            + "JOIN article_tag at2 ON at2.tag_id = t.id "
            + "JOIN article a ON a.id = at2.article_id AND a.status = 1 "
            + "GROUP BY t.id, t.name "
            + "ORDER BY article_count DESC, t.id ASC "
            + "LIMIT 100")
    List<TagVO> selectTagCloud();
}
