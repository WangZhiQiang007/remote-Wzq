import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Scanner;

/**
 * @Author WangZhiQiang
 * @Date 20242024/8/117:06
 * @Version HT
 */
@Slf4j
public class Te1 {
	@Test
	public void TimeInterval(){
		LocalDateTime date1 = LocalDateTime.of(2023, 6, 1, 20, 30);
		LocalDateTime date2 = LocalDateTime.of(2024, 8, 1, 17, 30);
		
		long days = getDaysBetween(date1, date2);
		Duration duration = getTimeDifference(date1, date2);
		
		System.out.println("Days between dates: " + days);
		System.out.println("Time difference: " + duration);
	}
	/**
	*@Caption
	*@Parm LocalDateTime 类型的日期 date1，date2；
	*@Return Duration 类型的时间差
	*/
	public static long getDaysBetween(LocalDateTime date1, LocalDateTime date2) {
		LocalDate startDate = date1.toLocalDate();
		LocalDate endDate = date2.toLocalDate();
		return ChronoUnit.DAYS.between(startDate, endDate);
	}
	
	public static Duration getTimeDifference(LocalDateTime date1, LocalDateTime date2) {
		Duration between = Duration.between(date1, date2);
		return between.abs();
	}
	@Test
	public void Test(){
		//接收键盘输入
		Scanner scanner = new Scanner(System.in);
		log.info("请输入可兑换的额度和所需要的凭证");
		Double i = (double) scanner.nextInt();
		log.info("请输入所需要的额度");
		Double i1 = Double.valueOf(scanner.nextInt());
		if (i/i1 == 2.00/3.00){
		log.info("可兑换的额度"+i);
		log.info("所需要的凭证"+i1);
		}else {
			log.info("不可兑换");
			log.info("请重新输入");
			Test();
		}
	}
}
