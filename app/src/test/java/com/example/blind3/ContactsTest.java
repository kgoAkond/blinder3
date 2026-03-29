package com.example.blind3;

import static org.junit.Assert.*;
import com.example.blind3.model.Contact;
import org.junit.Before;
import org.junit.Test;
import java.util.List;

public class ContactsTest {

    private Contacts contactsManager;

    @Before
    public void setUp() {
        contactsManager = new Contacts();
    }

    @Test
    public void testGetContact_ReturnsFirstContactOnFirstPress() {
        // Key 1 has: Alinka (prio 0), Kasia (prio 1), Wojtek (prio 2)
        Contact result = contactsManager.getContact(1);

        assertNotNull(result);
        assertEquals("Alinka", result.name());
    }

    @Test
    public void testGetContact_RotatesOnMultiplePressesOfSameKey() {
        // First press: Alinka
        Contact result1  = contactsManager.getContact(1);
        // Second press: Kasia
        Contact result2 = contactsManager.getContact(1);
        // Third press: Wojtek
        Contact result3 = contactsManager.getContact(1);
        // Fourth press: Should wrap around back to Alinka
        Contact result4 = contactsManager.getContact(1);

        assertEquals("Alinka", result1.name());
        assertEquals("Kasia", result2.name());
        assertEquals("Wojtek", result3.name());
        assertEquals("Alinka", result4.name());
    }

    @Test
    public void testGetContact_ResetsStateWhenSwitchingKeys() {
        // Press Key 1 -> Alinka
        contactsManager.getContact(1);
        // Press Key 1 -> Kasia
        contactsManager.getContact(1);

        // Switch to Key 2 -> Amelka
        Contact key2Result = contactsManager.getContact(2);
        assertEquals("Amelka", key2Result.name());

        // Switch back to Key 1 -> Should reset and return Alinka (not Wojtek)
        Contact key1ResetResult = contactsManager.getContact(1);
        assertEquals("Alinka", key1ResetResult.name());
    }

    @Test
    public void testGetContact_ReturnsNullForInvalidKey() {
        // Key 0 has no contacts in your provided list
        Contact result = contactsManager.getContact(0);
        assertNull(result);
    }

    @Test
    public void testPrioritySorting() {
        // This indirectly tests the private getContacts method
        // Key 2 in your list:
        // Amelka (prio 0), Konrad (prio 1), Zygmunt (prio 2)
        // If the sorting was broken, they might come out in a different order.

        assertEquals("Amelka", contactsManager.getContact(2).name());
        assertEquals("Konrad", contactsManager.getContact(2).name());
        assertEquals("Zygmunt", contactsManager.getContact(2).name());
    }
}