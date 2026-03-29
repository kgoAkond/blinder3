package com.example.blind3;

import com.example.blind3.model.Contact;

import java.util.ArrayList;
import java.util.List;

public class Contacts {
    public final static List<Contact> contacts = List.of(
            new Contact("Alinka", "600059039", 1, 0),
            new Contact("Kasia", "505944684", 1, 1),
            new Contact("Wojtek", "502425858", 1, 2),
            new Contact("Amelka", "732363027", 2, 0),
            new Contact("Konrad", "888868868", 2, 1),
            new Contact("Zygmunt", "601653315", 2, 2),
            new Contact("Barbara Mama", "792005298", 3, 0),
            new Contact("Łukasz", "665214527", 3, 1),
            new Contact("Basia", "501502996", 4, 0),
            new Contact("Magda Konopko", "606301411", 4, 1),
            new Contact("Jasia Brenienek", "664250942", 5, 0),
            new Contact("Przychodnia Smolec", "713111811", 5, 1),
            new Contact("Ewa Górska", "537682266", 6, 0),
            new Contact("Tadeusz Pióro", "784184048", 6, 1),
            new Contact("Irek", "577081762", 7, 0),
            new Contact("Tymon Godzwoń", "693565095", 7, 1),
            new Contact("Jasia Pańczyk", "691519471", 8, 0),
            new Contact("Weronika", "665811500", 8, 1),
            new Contact("Jurek Praca", "661406461", 9, 0),
            new Contact("Wito", "518730114", 9, 1)
    );

    private int state = 0;
    private int lastKey = -1;
    private Contact selectedContact = null;
    private long lastSelectedTime = -1;


    public Contact getContact(int key) {
        if (lastKey != key) {
            state = 0;
        }
        lastKey = key;
        var contactsKey = getContacts(key);
        if (contactsKey.isEmpty()) return null;
        selectedContact = contactsKey.get(state % contactsKey.size());
        lastSelectedTime = System.currentTimeMillis();
        state++;
        return selectedContact;
    }

    private List<Contact> getContacts(int key) {
        List<Contact> resp = new ArrayList<>();
        for (var c : contacts) {
            if (c.key() == key) {
                resp.add(c);
            }
        }
        resp.sort((o1, o2) -> o1.priority() - o2.priority());
        return resp;
    }
}
