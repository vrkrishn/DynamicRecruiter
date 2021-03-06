/*
 * Copyright (c) 2013 Mathew Gray.
 * This work is licensed under a Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.
 */

package com.phideltcmu.recruiter.server.dao;

import com.phideltcmu.recruiter.server.dao.mapper.*;
import com.phideltcmu.recruiter.server.directory.CmuLdap;
import com.phideltcmu.recruiter.shared.model.*;
import com.unboundid.ldap.sdk.LDAPException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

public class RecruitListDao implements IDao {
    private DataSource dataSource;
    private JdbcTemplate jdbcTemplate = null;

    private void checkSingleton() {
        if (jdbcTemplate == null) {
            jdbcTemplate = new JdbcTemplate(dataSource);
        }
    }

    @Override
    public void setDataSource(DataSource ds) {
        this.dataSource = ds;
    }

    @Override
    public void create(String firstName, String lastName, String andrewID) {
        checkSingleton();
        jdbcTemplate.update(
                "INSERT INTO recruitList.infolist VALUES (?, ?, ?, default, default, default, default, default, default)",
                new Object[]{firstName, lastName, andrewID});
    }

    @Override
    public List<Person> select(String andrewID) {
        checkSingleton();
        List<Person> results = jdbcTemplate.query("SELECT * FROM recruitList.infolist WHERE andrewid=?",
                new Object[]{andrewID}, new PersonRowMapper());
        return results.size() == 0 ? null : results;
    }

    @Override
    public List<Person> selectAll(List<Category> desiredCategories) {
        checkSingleton();

        List<String> list = new ArrayList<String>();
        for (Category c : desiredCategories) {
            list.add(c.getValue());
        }

        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("categories", list);
        return namedParameterJdbcTemplate.query("SELECT * FROM recruitList.infolist WHERE status IN (:categories)",
                parameters,
                new PersonRowMapper()
        );
    }

    @Override
    public void delete(String andrewId) {
        checkSingleton();
        jdbcTemplate.update("DELETE FROM recruitList.infolist WHERE andrewid=?",
                new Object[]{andrewId});
    }

    @Override
    public boolean register(AuthUser user) {
        List<InternalUser> internalMatches = getInternalUser(user.getId());

        if (internalMatches.size() == 0) {
            jdbcTemplate.update("INSERT INTO recruitList.userList VALUES (default,?,default,?)",
                    new Object[]{user.getFullName(), user.getId()});

            return true;
        }
        return false;
    }

    @Override
    public void updateList() throws LDAPException {
        checkSingleton();
        List<Person> matches = jdbcTemplate.query("SELECT * FROM recruitList.infolist",
                new PersonRowMapper());
        for (Person p : matches) {
            Person newPerson = CmuLdap.getAttributesStrictlyByAndrewID(p.getAndrewID());
            /**
             * Delete people that are no longer at CMU
             * (also deletes if no longer undergrad)
             */
            if (newPerson == null) {
                System.out.println("Deleting andrew id " + p.getAndrewID() + " from table");
                delete(p.getAndrewID());
                continue;
            }
            jdbcTemplate.update("UPDATE recruitList.infolist SET lastname=?,firstname=?,classyear=?,major=? WHERE andrewid=?",
                    new Object[]{newPerson.getLastName(), newPerson.getFirstName(), newPerson.getClassYear(), newPerson.getMajor(), newPerson.getAndrewID()});
        }
    }

    @Override
    public boolean add(Person p, AuthUser user) {
        checkSingleton();
        List<Person> matches = jdbcTemplate.query("SELECT * FROM recruitList.infolist WHERE andrewid=?",
                new Object[]{p.getAndrewID()},
                new PersonRowMapper());

        if (matches.size() != 0) {
            System.out.println("Already in DB");
            return false;
        }

        int id = getInternalUser(user.getId()).get(0).getDatabaseID();

        jdbcTemplate.update("INSERT INTO recruitList.infolist VALUES (?, ?, ?, default, ?, ?, default, default, default, ?)",
                new Object[]{p.getLastName(), p.getFirstName(), p.getAndrewID(), p.getMajor(), p.getClassYear(), id});

        return true;
    }

    @Override
    public boolean addCategory(String categoryName) {
        checkSingleton();

        List<Person> matches = jdbcTemplate.query("SELECT * FROM recruitList.statuses WHERE status=?",
                new Object[]{categoryName},
                new PersonRowMapper());

        if (matches.size() != 0) {
            return false;
        } else {
            jdbcTemplate.update("INSERT INTO recruitList.statuses VALUES (?)",
                    new Object[]{categoryName});
            return true;
        }
    }

    @Override
    public List<Category> getCategories() {
        checkSingleton();
        return jdbcTemplate.query("SELECT * FROM recruitList.statuses",
                new CategoryRowMapper());
    }

    @Override
    public void changeCategory(String andrewID, String newStatus) {
        checkSingleton();
        jdbcTemplate.update("UPDATE recruitList.infolist SET status=? WHERE andrewid=?",
                new Object[]{newStatus, andrewID});
    }

    @Override
    public void saveNotes(String andrewID, String notes) {
        checkSingleton();
        jdbcTemplate.update("UPDATE recruitList.infolist SET notes=? WHERE andrewid=?",
                new Object[]{notes, andrewID});
    }

    @Override
    public String addToReferrals(String andrewid, String fbid) {
        checkSingleton();

        InternalUser iu = getInternalUser(fbid).get(0);


        List<String> referrals = jdbcTemplate.query("SELECT additionalReferrals FROM recruitList.infolist WHERE andrewid=?",
                new Object[]{andrewid}, new AdditionalReferralRowMapper());

        if (referrals.get(0).contains(Integer.toString(iu.getDatabaseID()))) {
            return "Already Referred";
        }

        String appendString = (getInternalUser(fbid).get(0).getDatabaseID()) + ",";
        jdbcTemplate.update("UPDATE recruitList.infolist SET additionalReferrals= CONCAT(additionalReferrals,?) WHERE andrewid=?",
                new Object[]{appendString, andrewid});
        return "Already Exists - Reference Added";
    }

    private List<InternalUser> getInternalUser(String fbID) {
        return jdbcTemplate.query("SELECT * FROM recruitList.userList WHERE facebookID=?",
                new Object[]{fbID},
                new InternalUserRowMapper());
    }

    @Override
    public String getNameFromInternalID(String internalID) {
        checkSingleton();
        List<String> singletonList = jdbcTemplate.query("SELECT name FROM recruitList.userList WHERE id=?",
                new Object[]{internalID},
                new InternalNameRowMapper());

        if (singletonList.size() > 0) {
            return singletonList.get(0);
        }
        return "ERROR";
    }

    @Override
    public void updateTelephone(String andrewID, String phoneNumber) {
        checkSingleton();
        jdbcTemplate.update("UPDATE recruitList.infolist SET phonenumber=? WHERE andrewid=?",
                new Object[]{phoneNumber, andrewID});
    }

    @Override
    public boolean isAdmin(String fbid) {
        checkSingleton();
        List<Boolean> booleanList = jdbcTemplate.query("SELECT isAdmin FROM recruitList.userList WHERE facebookID = ?",
                new Object[]{fbid}, new IsAdminRowMapper());
        if (booleanList.size() > 0) {
            return booleanList.get(0);
        }
        return false;
    }

    @Override
    public List<InternalUser> getNonAdmins() {
        checkSingleton();
        return jdbcTemplate.query("SELECT * FROM recruitList.userList WHERE isAdmin=0",
                new InternalUserRowMapper());
    }

    @Override
    public List<InternalUser> getAdmins() {
        checkSingleton();
        return jdbcTemplate.query("SELECT * FROM recruitList.userList WHERE isAdmin=1",
                new InternalUserRowMapper());
    }

    @Override
    public void setAdmin(String fbid, Boolean b) {
        checkSingleton();
        jdbcTemplate.update("UPDATE recruitList.userList SET isAdmin=? WHERE facebookID=?",
                new Object[]{b, fbid});
    }

    @Override
    public List<InternalUserStat> getStats() {
        checkSingleton();
        List<InternalUserStat> posAdditions = jdbcTemplate.query("SELECT referredBy, COUNT(*) FROM recruitList.infolist GROUP BY referredBy ORDER BY COUNT(*) DESC",
                new StatCountRowMapper());

        List<InternalUserStat> zeroAdditons = jdbcTemplate.query("SELECT id FROM recruitList.userList",
                new StatZeroRowMapper());

        /**
         * Get the users with 0 additions
         * This will pr
         */
        for (InternalUserStat s : zeroAdditons) {
            if (!posAdditions.contains(s)) {
                posAdditions.add(s);
            }
        }

        return posAdditions;
    }
}
