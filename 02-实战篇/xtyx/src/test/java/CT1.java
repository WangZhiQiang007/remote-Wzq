import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Service;
import xtyx.XTYX_Application;
import xtyx.entity.User;
import xtyx.mapper.UserMapper;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
@SpringBootTest(classes = XTYX_Application.class)
@Service
public class CT1 extends ServiceImpl<UserMapper,User> implements IService<User>  {
	@Test
	public void test() {
		User user = new User();
		user.setPassword("13422223242");
		user.setNickName("扬帆起航-05");
		user.setPhone("13422223242");
		user.setCreateBy("Wzq02");
		lambdaUpdate()
				.eq(User::getPhone,"13323232323")
				.set(User::getPassword,user.getPassword())
				.update();
		update()
				.eq("phone","13323232323")
				.set("password",user.getPassword())
				.update();
//		通过new对象进行修改
		update()
				.eq("phone","13323232323")
				.update(user);
	}
	
	@Override
	public boolean saveBatch(Collection<User> entityList, int batchSize) {
		return false;
	}
	
	@Override
	public boolean saveOrUpdateBatch(Collection<User> entityList, int batchSize) {
		return false;
	}
	
	@Override
	public boolean updateBatchById(Collection<User> entityList, int batchSize) {
		return false;
	}
	
	@Override
	public boolean saveOrUpdate(User entity) {
		return false;
	}
	
	@Override
	public User getOne(Wrapper<User> queryWrapper, boolean throwEx) {
		return null;
	}
	
	@Override
	public Map<String, Object> getMap(Wrapper<User> queryWrapper) {
		return null;
	}
	
	@Override
	public <V> V getObj(Wrapper<User> queryWrapper, Function<? super Object, V> mapper) {
		return null;
	}
	
	
	
	@Override
	public Class<User> getEntityClass() {
		return null;
	}
}
