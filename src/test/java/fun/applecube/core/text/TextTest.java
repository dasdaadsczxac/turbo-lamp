package fun.applecube.core.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextTest {

    @Test
    void legacyColoursBecomeTags() {
        assertEquals("<gold><bold>Hi", Text.legacyToMiniMessage("&6&lHi"));
        assertEquals("<#d3773a>x", Text.legacyToMiniMessage("&#d3773ax"));
        assertEquals("<#d3773a>x", Text.legacyToMiniMessage("&x&d&3&7&7&3&ax"));
        assertEquals("<#E48518>x", Text.legacyToMiniMessage("§x§E§4§8§5§1§8x"));
    }

    @Test
    void colourAfterDecorationResets() {
        assertEquals("<bold>a<reset><red>b", Text.legacyToMiniMessage("&la&cb"));
    }

    @Test
    void plainAmpersandsStay() {
        assertEquals("Tom & Jerry &z", Text.legacyToMiniMessage("Tom & Jerry &z"));
        assertEquals("<accent>no codes", Text.legacyToMiniMessage("<accent>no codes"));
    }

    @Test
    void packSections() {
        String raw = "<pack>P</pack><plain>V</plain>!";
        assertEquals("P!", Text.applyPackSections(raw, true));
        assertEquals("V!", Text.applyPackSections(raw, false));
        assertEquals("a$1b", Text.applyPackSections("<pack>a$1b</pack>", true));
    }

    @Test
    void unicodeEscapes() {
        assertEquals("\uEB40\uEB98", Text.unescapeUnicode("\\uEB40\\uEB98"));
        assertEquals("plain", Text.unescapeUnicode("plain"));
    }
}
