package org.example.handlers;

import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Used to print Error response to TCP.
 * It builds a new line character seperated header, body and footer.
 */
@Builder
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
public class ErrorResponse {

    private static final String NEWLINE = "\n";

    private String header;
    private String body;
    private String footer;

    /**
     * Builds a multi line string with header, body and footer, each in new line.
     *
     * @return String
     */
    public String getMessage() {
        final StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(header)) {
            builder.append(header);
            builder.append(NEWLINE);
        }
        if (StringUtils.hasText(body)) {
            builder.append(body);
            builder.append(NEWLINE);
        }
        if (StringUtils.hasText(footer)) {
            builder.append(footer);
        }

        return builder.toString();
    }
}
