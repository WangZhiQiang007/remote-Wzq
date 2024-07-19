package xtyx.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import xtyx.dto.Result;
import xtyx.dto.ScrollResult;
import xtyx.dto.UserDTO;
import xtyx.entity.Blog;
import xtyx.entity.Follow;
import xtyx.entity.User;
import xtyx.mapper.BlogMapper;
import xtyx.service.IBlogService;
import org.springframework.stereotype.Service;
import xtyx.service.IFollowService;
import xtyx.service.IUserService;
import xtyx.utils.UserHolder;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static xtyx.utils.RedisConstants.BLOG_LIKED_KEY;


@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {
	
	@Resource
	private IUserService userService;
	@Resource
	private StringRedisTemplate stringRedisTemplate;
	@Resource
	private IFollowService followService;
	
	@Override
	public Result queryBlogById(Long id) {
		Blog blog = getById(id);
		if (blog == null) {
			return Result.fail("笔记不存在");
		}
		queryBlogUser(blog);
		isBlogLiked(blog);
		return Result.ok(blog);
	}
	
	private void isBlogLiked(Blog blog) {
		// 获取登录用户
		UserDTO user = UserHolder.getUser();
		if (user == null){
			return;
		}
		Long userId = user.getId();
		// 判断当前用户是否已经点赞
		Double isMember = stringRedisTemplate.opsForZSet().score(BLOG_LIKED_KEY + blog.getId(), userId.toString());
		blog.setIsLike(isMember != null);
	}
	
	@Override
	public Result queryHotBlog(Integer current) {
		// 根据用户查询
		Page<Blog> page = query()
								  .orderByDesc("liked")
								  .page(new Page<>(current, 20));
		// 获取当前页数据
		List<Blog> records = page.getRecords();
		// 查询用户
		records.forEach(blog -> {
			queryBlogUser(blog);
			isBlogLiked(blog);
		});
		return Result.ok(records);
	}
	
	@Override
	public Result likeBlog(Long id) {
		// 获取登录用户
		Long userId = UserHolder.getUser().getId();
		// 判断当前用户是否已经点赞
		Double score = stringRedisTemplate.opsForZSet().score(BLOG_LIKED_KEY + id, userId.toString());
		if (score == null) {
			// 如果未点赞，可以点赞
			// 数据库点赞数 + 1
			boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
			if (isSuccess) {
				// 保存用户到redis的set集合
				stringRedisTemplate.opsForZSet().add(BLOG_LIKED_KEY + id, userId.toString(), System.currentTimeMillis());
			}
			
		} else{
			// 如果已点赞，取消点赞
			boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
			if (isSuccess) {
				// 从redis的set集合中移除用户
				stringRedisTemplate.opsForZSet().remove(BLOG_LIKED_KEY + id, userId.toString());
			}
		}
		
		return Result.ok();
	}
	
	@Override
	public Result queryBlogLikes(Long id) {
		Set<String> top5 = stringRedisTemplate.opsForZSet().range(BLOG_LIKED_KEY + id, 0, 4);
		if (top5 == null || top5.isEmpty()){
			return Result.ok(Collections.emptyList());
		}
		List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
		String idStr = StrUtil.join(",", ids);
		List<UserDTO> collect = userService.query()
										.in("id",ids).last("order by field(id,"+ idStr +")").list()
									   	.stream()
									   	.map(user -> BeanUtil.copyProperties(user, UserDTO.class))
									   	.collect(Collectors.toList());
		return Result.ok(collect);
		
	}
	
	@Override
	public Result saveBlog(Blog blog) {
		UserDTO user = UserHolder.getUser();
		blog.setUserId(user.getId());
		boolean isSuccess = save(blog);
		if (isSuccess){
			return Result.fail("新增笔记失败！");
		}
		List<Follow> follows = followService.query().eq("follow_user_id", user.getId()).list();
		for(Follow follow : follows){
			//获取粉丝id
			Long followUserId = follow.getUserId();
			//推送
			stringRedisTemplate.opsForZSet().add("feed:" + followUserId, blog.getId().toString(), System.currentTimeMillis());
		}
		//返回id
		return Result.ok(blog.getId());
	}
	
	@Override
	public Result queryBlogOfFollow(Long max, Integer offset) {
		//获取当前用户
		Long userId = UserHolder.getUser().getId();
		//查询收件箱
		Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
																	 .reverseRangeByScoreWithScores("feed:" + userId, 0, max, offset, 2);
		if (typedTuples == null || typedTuples.isEmpty()){
			return  Result.ok();
		}
		//解析数据：1.时间戳 2.blogId 3.offset
		List<Long> ids = new ArrayList<>(typedTuples.size());
		long minTime = 0;
		int os = 1;
		for (ZSetOperations.TypedTuple<String> tuple:typedTuples){
			ids.add(Long.valueOf(tuple.getValue()));
			long time = tuple.getScore().longValue();
			if (time == minTime){
				os++;
			}else{
				minTime = time;
				os = 1;
			}
		}
		String idStr = StrUtil.join(",", ids);
		List<Blog> blogs = query().in("id",ids).last("ORDER BY FIELD(id,"+idStr+")").list();
	//封装结果返回
		ScrollResult scrollResult = new ScrollResult();
		scrollResult.setList(blogs);
		scrollResult.setOffset(os);
		scrollResult.setMinTime(minTime);
		return Result.ok(scrollResult);
	}
	
	private void queryBlogUser(Blog blog) {
		Long userId = blog.getUserId();
		User user = userService.getOne(new QueryWrapper<User>().eq("id", userId));
		if (user == null){
			throw new RuntimeException("用户不存在");
		}
		blog.setName(user.getNickName());
		blog.setIcon(user.getIcon());
	}
}
