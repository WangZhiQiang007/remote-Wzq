package xtyx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import xtyx.dto.Result;
import xtyx.entity.Blog;


public interface IBlogService extends IService<Blog> {
	
	Result queryBlogById(Long id);
	
	Result queryHotBlog(Integer current);
	
	Result likeBlog(Long id);
	
	Result queryBlogLikes(Long id);
	
	Result saveBlog(Blog blog);
	
	Result queryBlogOfFollow(Long max, Integer offset);
}
