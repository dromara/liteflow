package com.yomahub.liteflow.agent.component;

import com.yomahub.liteflow.slot.Slot;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ReActAgentContextTest {

    @Test
    void exposes_slot_sessionid_workspace() {
        Slot slot = Mockito.mock(Slot.class);
        Path ws = Path.of("/tmp/ws");
        ReActAgentContext ctx = new ReActAgentContext(slot, "sess-1", ws);

        assertSame(slot, ctx.getSlot());
        assertEquals("sess-1", ctx.getSessionId());
        assertEquals(ws, ctx.getWorkspaceDir());
    }

    @Test
    void rejects_null_required_fields() {
        Slot slot = Mockito.mock(Slot.class);
        assertThrows(NullPointerException.class,
                () -> new ReActAgentContext(null, "s", Path.of("/t")));
        assertThrows(NullPointerException.class,
                () -> new ReActAgentContext(slot, null, Path.of("/t")));
        assertThrows(NullPointerException.class,
                () -> new ReActAgentContext(slot, "s", null));
    }
}
