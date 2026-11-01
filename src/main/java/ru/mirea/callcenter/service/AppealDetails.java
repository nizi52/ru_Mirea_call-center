package ru.mirea.callcenter.service;

import ru.mirea.callcenter.model.*;

/** Обращение вместе с клиентом и оператором (оператор может быть не назначен). */
public record AppealDetails(Appeal appeal, Client client, Operator operator) { }
