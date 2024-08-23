package utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class GeneralUtils {

    public static double roundDouble(double value, int decimals) {
        BigDecimal bd = new BigDecimal(value).setScale(decimals, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
    
}
