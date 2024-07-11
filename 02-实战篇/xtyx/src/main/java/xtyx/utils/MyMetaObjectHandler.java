package xtyx.utils;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;

import java.util.Date;

public class MyMetaObjectHandler implements MetaObjectHandler {
	@Override
	public void insertFill(MetaObject metaObject) {
		this.strictInsertFill(metaObject, "createTime",Date.class, new Date());
		this.strictUpdateFill(metaObject, "updateTime",Date.class,new Date());
		this.strictInsertFill(metaObject, "createBy",String.class,"Wzq-01");
	}
	
	@Override
	public void updateFill(MetaObject metaObject) {
		this.strictInsertFill(metaObject, "updateTime",Date.class, new Date());
	}
}
