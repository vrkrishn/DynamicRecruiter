/*
 * Copyright (c) 2013 Mathew Gray.
 * This work is licensed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.
 */

package com.phideltcmu.recruiter.server.dao;


import com.phideltcmu.recruiter.shared.model.*;
import com.unboundid.ldap.sdk.LDAPException;

import javax.sql.DataSource;
import java.util.List;


public interface IDao {

    void setDataSource(DataSource ds);

    void create(String firstName, String lastName, String andrewID);

    List<Person> select(String andrewId);

    List<Person> selectAll(List<Category> desiredCategories);

    void delete(String andrewID);

    boolean add(Person p, AuthUser token);

    boolean addCategory(String categoryName);

    List<Category> getCategories();

    void changeCategory(String andrewID, String newStatus);

    void saveNotes(String andrewID, String Notes);

    String addToReferrals(String andrewID, String id);

    String getNameFromInternalID(String internalID);

    void updateTelephone(String andrewID, String phoneNumber);

    boolean isAdmin(String fbid);

    List<InternalUser> getNonAdmins();

    List<InternalUser> getAdmins();

    void setAdmin(String fbid, Boolean b);

    List<InternalUserStat> getStats();

    boolean register(AuthUser user);

    void updateList() throws LDAPException;
}
