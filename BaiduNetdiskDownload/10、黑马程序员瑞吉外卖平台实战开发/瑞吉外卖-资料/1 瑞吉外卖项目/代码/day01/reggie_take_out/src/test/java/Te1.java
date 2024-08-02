import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * @Author WangZhiQiang
 * @Date 20242024/8/117:06
 * @Version HT
 */
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
	*@Parm
	*@Return
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
}
