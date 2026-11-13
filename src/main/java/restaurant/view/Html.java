package restaurant.view;

import java.text.NumberFormat;
import java.util.Locale;

final class Html {
    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");

    private Html() {
    }

    static String money(long amount) {
        return NumberFormat.getIntegerInstance(VIETNAMESE).format(amount);
    }

    static String escape(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
