package com.liuliu.example.myblogbackend.service;

import com.liuliu.example.myblogbackend.dto.TagVO;
import com.liuliu.example.myblogbackend.mapper.TagMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TagService {

    @Autowired
    private TagMapper tagMapper;

    /** 标签云：标签 + 已发布文章数（一条 group by SQL） */
    public List<TagVO> tagCloud() {
        return tagMapper.selectTagCloud();
    }
}
