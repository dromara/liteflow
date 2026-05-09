package com.yomahub.liteflow.test.slot;

import com.yomahub.liteflow.slot.Slot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SlotAttachmentTest {

	@Test
	public void setAndGetAttachment_roundTrip() {
		Slot slot = new Slot();
		slot.setAttachment("foo", "bar");
		String v = slot.getAttachment("foo");
		assertEquals("bar", v);
	}

	@Test
	public void getAttachment_missingKey_returnsNull() {
		Slot slot = new Slot();
		Object v = slot.getAttachment("absent");
		assertNull(v);
	}

	@Test
	public void hasAttachment_reportsPresence() {
		Slot slot = new Slot();
		assertFalse(slot.hasAttachment("k"));
		slot.setAttachment("k", 1);
		assertTrue(slot.hasAttachment("k"));
	}

	@Test
	public void removeAttachment_clearsValue() {
		Slot slot = new Slot();
		slot.setAttachment("k", 1);
		slot.removeAttachment("k");
		assertFalse(slot.hasAttachment("k"));
		assertNull(slot.getAttachment("k"));
	}

	@Test
	public void setAttachment_nullValue_throws() {
		Slot slot = new Slot();
		assertThrows(RuntimeException.class, () -> slot.setAttachment("k", null));
	}

	@Test
	public void getAttachment_genericTypeInference() {
		Slot slot = new Slot();
		slot.setAttachment("num", 42);
		Integer n = slot.getAttachment("num");
		assertEquals(42, n);
	}
}
