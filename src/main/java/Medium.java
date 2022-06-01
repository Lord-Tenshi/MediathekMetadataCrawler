import lombok.*;

import java.io.Serializable;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
@ToString
public class Medium implements Serializable {
    String title;
    String description;
    List<String> tags;
    List<String> categories;
}
