package xtyx.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import xtyx.entity.BlogComments;
import xtyx.mapper.BlogCommentsMapper;
import xtyx.service.IBlogCommentsService;
import org.springframework.stereotype.Service;


@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

}
