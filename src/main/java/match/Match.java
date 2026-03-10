package match;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import wrestler.Wrestler;

@Setter
@Getter
@NoArgsConstructor
public class Match {

  private Wrestler wrestlerOne;
  private Wrestler wrestlerTwo;

  @Override
  public String toString() {
    return String.format("%s vs %s", wrestlerOne, wrestlerTwo);
  }
}
