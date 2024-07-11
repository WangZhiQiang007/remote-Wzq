package xtyx.utils;

public interface ILock {
/*
 * 尝试获取锁
 */
	Boolean tryLock(Long timeoutSec);
	/*
	 * 释放锁
	 */
	void unlock();
}
