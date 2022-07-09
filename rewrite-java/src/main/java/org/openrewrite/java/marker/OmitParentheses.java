package org.openrewrite.java.marker;

import lombok.Value;
import lombok.With;
import org.openrewrite.marker.Marker;

import java.util.UUID;

@Value
@With
public class OmitParentheses implements Marker {
    UUID id;
}