/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.providers.contacts;

import com.android.internal.util.ArrayUtils;
import com.android.providers.contacts.ContactsDatabaseHelper.AggregationExceptionColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.DataUsageStatColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.PresenceColumns;
import com.android.providers.contacts.tests.R;
import com.google.android.collect.Lists;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Entity;
import android.content.EntityIterator;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.provider.ContactsContract.AggregationExceptions;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.ContactCounts;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.DataUsageFeedback;
import android.provider.ContactsContract.Directory;
import android.provider.ContactsContract.DisplayNameSources;
import android.provider.ContactsContract.DisplayPhoto;
import android.provider.ContactsContract.FullNameStyle;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.PhoneticNameStyle;
import android.provider.ContactsContract.Profile;
import android.provider.ContactsContract.ProviderStatus;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.RawContactsEntity;
import android.provider.ContactsContract.SearchSnippetColumns;
import android.provider.ContactsContract.Settings;
import android.provider.ContactsContract.StatusUpdates;
import android.provider.ContactsContract.StreamItemPhotos;
import android.provider.ContactsContract.StreamItems;
import android.provider.OpenableColumns;
import android.test.MoreAsserts;
import android.test.suitebuilder.annotation.LargeTest;
import android.text.TextUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Unit tests for {@link ContactsProvider2}.
 *
 * Run the test like this:
 * <code>
   adb shell am instrument -e class com.android.providers.contacts.ContactsProvider2Test -w \
           com.android.providers.contacts.tests/android.test.InstrumentationTestRunner
 * </code>
 */
@LargeTest
public class ContactsProvider2Test extends BaseContactsProvider2Test {

    private static final Account ACCOUNT_1 = new Account("account_name_1", "account_type_1");
    private static final Account ACCOUNT_2 = new Account("account_name_2", "account_type_2");

    public void testContactsProjection() {
        assertProjection(Contacts.CONTENT_URI, new String[]{
                Contacts._ID,
                Contacts.DISPLAY_NAME_PRIMARY,
                Contacts.DISPLAY_NAME_ALTERNATIVE,
                Contacts.DISPLAY_NAME_SOURCE,
                Contacts.PHONETIC_NAME,
                Contacts.PHONETIC_NAME_STYLE,
                Contacts.SORT_KEY_PRIMARY,
                Contacts.SORT_KEY_ALTERNATIVE,
                Contacts.LAST_TIME_CONTACTED,
                Contacts.TIMES_CONTACTED,
                Contacts.STARRED,
                Contacts.IN_VISIBLE_GROUP,
                Contacts.PHOTO_ID,
                Contacts.PHOTO_FILE_ID,
                Contacts.PHOTO_URI,
                Contacts.PHOTO_THUMBNAIL_URI,
                Contacts.CUSTOM_RINGTONE,
                Contacts.HAS_PHONE_NUMBER,
                Contacts.SEND_TO_VOICEMAIL,
                Contacts.IS_USER_PROFILE,
                Contacts.LOOKUP_KEY,
                Contacts.NAME_RAW_CONTACT_ID,
                Contacts.CONTACT_PRESENCE,
                Contacts.CONTACT_CHAT_CAPABILITY,
                Contacts.CONTACT_STATUS,
                Contacts.CONTACT_STATUS_TIMESTAMP,
                Contacts.CONTACT_STATUS_RES_PACKAGE,
                Contacts.CONTACT_STATUS_LABEL,
                Contacts.CONTACT_STATUS_ICON,
        });
    }

    public void testContactsWithSnippetProjection() {
        assertProjection(Contacts.CONTENT_FILTER_URI.buildUpon().appendPath("nothing").build(),
            new String[]{
                Contacts._ID,
                Contacts.DISPLAY_NAME_PRIMARY,
                Contacts.DISPLAY_NAME_ALTERNATIVE,
                Contacts.DISPLAY_NAME_SOURCE,
                Contacts.PHONETIC_NAME,
                Contacts.PHONETIC_NAME_STYLE,
                Contacts.SORT_KEY_PRIMARY,
                Contacts.SORT_KEY_ALTERNATIVE,
                Contacts.LAST_TIME_CONTACTED,
                Contacts.TIMES_CONTACTED,
                Contacts.STARRED,
                Contacts.IN_VISIBLE_GROUP,
                Contacts.PHOTO_ID,
                Contacts.PHOTO_FILE_ID,
                Contacts.PHOTO_URI,
                Contacts.PHOTO_THUMBNAIL_URI,
                Contacts.CUSTOM_RINGTONE,
                Contacts.HAS_PHONE_NUMBER,
                Contacts.SEND_TO_VOICEMAIL,
                Contacts.IS_USER_PROFILE,
                Contacts.LOOKUP_KEY,
                Contacts.NAME_RAW_CONTACT_ID,
                Contacts.CONTACT_PRESENCE,
                Contacts.CONTACT_CHAT_CAPABILITY,
                Contacts.CONTACT_STATUS,
                Contacts.CONTACT_STATUS_TIMESTAMP,
                Contacts.CONTACT_STATUS_RES_PACKAGE,
                Contacts.CONTACT_STATUS_LABEL,
                Contacts.CONTACT_STATUS_ICON,

                SearchSnippetColumns.SNIPPET,
        });
    }

    public void testRawContactsProjection() {
        assertProjection(RawContacts.CONTENT_URI, new String[]{
                RawContacts._ID,
                RawContacts.CONTACT_ID,
                RawContacts.ACCOUNT_NAME,
                RawContacts.ACCOUNT_TYPE,
                RawContacts.DATA_SET,
                RawContacts.ACCOUNT_TYPE_AND_DATA_SET,
                RawContacts.SOURCE_ID,
                RawContacts.VERSION,
                RawContacts.RAW_CONTACT_IS_USER_PROFILE,
                RawContacts.DIRTY,
                RawContacts.DELETED,
                RawContacts.DISPLAY_NAME_PRIMARY,
                RawContacts.DISPLAY_NAME_ALTERNATIVE,
                RawContacts.DISPLAY_NAME_SOURCE,
                RawContacts.PHONETIC_NAME,
                RawContacts.PHONETIC_NAME_STYLE,
                RawContacts.NAME_VERIFIED,
                RawContacts.SORT_KEY_PRIMARY,
                RawContacts.SORT_KEY_ALTERNATIVE,
                RawContacts.TIMES_CONTACTED,
                RawContacts.LAST_TIME_CONTACTED,
                RawContacts.CUSTOM_RINGTONE,
                RawContacts.SEND_TO_VOICEMAIL,
                RawContacts.STARRED,
                RawContacts.AGGREGATION_MODE,
                RawContacts.SYNC1,
                RawContacts.SYNC2,
                RawContacts.SYNC3,
                RawContacts.SYNC4,
        });
    }

    public void testDataProjection() {
        assertProjection(Data.CONTENT_URI, new String[]{
                Data._ID,
                Data.RAW_CONTACT_ID,
                Data.DATA_VERSION,
                Data.IS_PRIMARY,
                Data.IS_SUPER_PRIMARY,
                Data.RES_PACKAGE,
                Data.MIMETYPE,
                Data.DATA1,
                Data.DATA2,
                Data.DATA3,
                Data.DATA4,
                Data.DATA5,
                Data.DATA6,
                Data.DATA7,
                Data.DATA8,
                Data.DATA9,
                Data.DATA10,
                Data.DATA11,
                Data.DATA12,
                Data.DATA13,
                Data.DATA14,
                Data.DATA15,
                Data.SYNC1,
                Data.SYNC2,
                Data.SYNC3,
                Data.SYNC4,
                Data.CONTACT_ID,
                Data.PRESENCE,
                Data.CHAT_CAPABILITY,
                Data.STATUS,
                Data.STATUS_TIMESTAMP,
                Data.STATUS_RES_PACKAGE,
                Data.STATUS_LABEL,
                Data.STATUS_ICON,
                RawContacts.ACCOUNT_NAME,
                RawContacts.ACCOUNT_TYPE,
                RawContacts.DATA_SET,
                RawContacts.ACCOUNT_TYPE_AND_DATA_SET,
                RawContacts.SOURCE_ID,
                RawContacts.VERSION,
                RawContacts.DIRTY,
                RawContacts.NAME_VERIFIED,
                RawContacts.RAW_CONTACT_IS_USER_PROFILE,
                Contacts._ID,
                Contacts.DISPLAY_NAME_PRIMARY,
                Contacts.DISPLAY_NAME_ALTERNATIVE,
                Contacts.DISPLAY_NAME_SOURCE,
                Contacts.PHONETIC_NAME,
                Contacts.PHONETIC_NAME_STYLE,
                Contacts.SORT_KEY_PRIMARY,
                Contacts.SORT_KEY_ALTERNATIVE,
                Contacts.LAST_TIME_CONTACTED,
                Contacts.TIMES_CONTACTED,
                Contacts.STARRED,
                Contacts.IN_VISIBLE_GROUP,
                Contacts.PHOTO_ID,
                Contacts.PHOTO_FILE_ID,
                Contacts.PHOTO_URI,
                Contacts.PHOTO_THUMBNAIL_URI,
                Contacts.CUSTOM_RINGTONE,
                Contacts.SEND_TO_VOICEMAIL,
                Contacts.LOOKUP_KEY,
                Contacts.NAME_RAW_CONTACT_ID,
                Contacts.HAS_PHONE_NUMBER,
                Contacts.CONTACT_PRESENCE,
                Contacts.CONTACT_CHAT_CAPABILITY,
                Contacts.CONTACT_STATUS,
                Contacts.CONTACT_STATUS_TIMESTAMP,
                Contacts.CONTACT_STATUS_RES_PACKAGE,
                Contacts.CONTACT_STATUS_LABEL,
                Contacts.CONTACT_STATUS_ICON,
                GroupMembership.GROUP_SOURCE_ID,
        });
    }

    public void testDistinctDataProjection() {
        assertProjection(Phone.CONTENT_FILTER_URI.buildUpon().appendPath("123").build(),
            new String[]{
                Data._ID,
                Data.DATA_VERSION,
                Data.IS_PRIMARY,
                Data.IS_SUPER_PRIMARY,
                Data.RES_PACKAGE,
                Data.MIMETYPE,
                Data.DATA1,
                Data.DATA2,
                Data.DATA3,
                Data.DATA4,
                Data.DATA5,
                Data.DATA6,
                Data.DATA7,
                Data.DATA8,
                Data.DATA9,
                Data.DATA10,
                Data.DATA11,
                Data.DATA12,
                Data.DATA13,
                Data.DATA14,
                Data.DATA15,
                Data.SYNC1,
                Data.SYNC2,
                Data.SYNC3,
                Data.SYNC4,
                Data.CONTACT_ID,
                Data.PRESENCE,
                Data.CHAT_CAPABILITY,
                Data.STATUS,
                Data.STATUS_TIMESTAMP,
                Data.STATUS_RES_PACKAGE,
                Data.STATUS_LABEL,
                Data.STATUS_ICON,
                RawContacts.RAW_CONTACT_IS_USER_PROFILE,
                Contacts._ID,
                Contacts.DISPLAY_NAME_PRIMARY,
                Contacts.DISPLAY_NAME_ALTERNATIVE,
                Contacts.DISPLAY_NAME_SOURCE,
                Contacts.PHONETIC_NAME,
                Contacts.PHONETIC_NAME_STYLE,
                Contacts.SORT_KEY_PRIMARY,
                Contacts.SORT_KEY_ALTERNATIVE,
                Contacts.LAST_TIME_CONTACTED,
                Contacts.TIMES_CONTACTED,
                Contacts.STARRED,
                Contacts.IN_VISIBLE_GROUP,
                Contacts.PHOTO_ID,
                Contacts.PHOTO_FILE_ID,
                Contacts.PHOTO_URI,
                Contacts.PHOTO_THUMBNAIL_URI,
                Contacts.HAS_PHONE_NUMBER,
                Contacts.CUSTOM_RINGTONE,
                Contacts.SEND_TO_VOICEMAIL,
                Contacts.LOOKUP_KEY,
                Contacts.CONTACT_PRESENCE,
                Contacts.CONTACT_CHAT_CAPABILITY,
                Contacts.CONTACT_STATUS,
                Contacts.CONTACT_STATUS_TIMESTAMP,
                Contacts.CONTACT_STATUS_RES_PACKAGE,
                Contacts.CONTACT_STATUS_LABEL,
                Contacts.CONTACT_STATUS_ICON,
                GroupMembership.GROUP_SOURCE_ID,
        });
    }

    public void testEntityProjection() {
        assertProjection(
            Uri.withAppendedPath(ContentUris.withAppendedId(Contacts.CONTENT_URI, 0),
                    Contacts.Entity.CONTENT_DIRECTORY),
            new String[]{
                Contacts.Entity._ID,
                Contacts.Entity.DATA_ID,
                Contacts.Entity.RAW_CONTACT_ID,
                Data.DATA_VERSION,
                Data.IS_PRIMARY,
                Data.IS_SUPER_PRIMARY,
                Data.RES_PACKAGE,
                Data.MIMETYPE,
                Data.DATA1,
                Data.DATA2,
                Data.DATA3,
                Data.DATA4,
                Data.DATA5,
                Data.DATA6,
                Data.DATA7,
                Data.DATA8,
                Data.DATA9,
                Data.DATA10,
                Data.DATA11,
                Data.DATA12,
                Data.DATA13,
                Data.DATA14,
                Data.DATA15,
                Data.SYNC1,
                Data.SYNC2,
                Data.SYNC3,
                Data.SYNC4,
                Data.CONTACT_ID,
                Data.PRESENCE,
                Data.CHAT_CAPABILITY,
                Data.STATUS,
                Data.STATUS_TIMESTAMP,
                Data.STATUS_RES_PACKAGE,
                Data.STATUS_LABEL,
                Data.STATUS_ICON,
                RawContacts.ACCOUNT_NAME,
                RawContacts.ACCOUNT_TYPE,
                RawContacts.DATA_SET,
                RawContacts.ACCOUNT_TYPE_AND_DATA_SET,
                RawContacts.SOURCE_ID,
                RawContacts.VERSION,
                RawContacts.DELETED,
                RawContacts.DIRTY,
                RawContacts.NAME_VERIFIED,
                RawContacts.SYNC1,
                RawContacts.SYNC2,
                RawContacts.SYNC3,
                RawContacts.SYNC4,
                Contacts._ID,
                Contacts.DISPLAY_NAME_PRIMARY,
                Contacts.DISPLAY_NAME_ALTERNATIVE,
                Contacts.DISPLAY_NAME_SOURCE,
                Contacts.PHONETIC_NAME,
                Contacts.PHONETIC_NAME_STYLE,
                Contacts.SORT_KEY_PRIMARY,
                Contacts.SORT_KEY_ALTERNATIVE,
                Contacts.LAST_TIME_CONTACTED,
                Contacts.TIMES_CONTACTED,
                Contacts.STARRED,
                Contacts.IN_VISIBLE_GROUP,
                Contacts.PHOTO_ID,
                Contacts.PHOTO_FILE_ID,
                Contacts.PHOTO_URI,
                Contacts.PHOTO_THUMBNAIL_URI,
                Contacts.CUSTOM_RINGTONE,
                Contacts.SEND_TO_VOICEMAIL,
                Contacts.IS_USER_PROFILE,
                Contacts.LOOKUP_KEY,
                Contacts.NAME_RAW_CONTACT_ID,
                Contacts.HAS_PHONE_NUMBER,
                Contacts.CONTACT_PRESENCE,
                Contacts.CONTACT_CHAT_CAPABILITY,
                Contacts.CONTACT_STATUS,
                Contacts.CONTACT_STATUS_TIMESTAMP,
                Contacts.CONTACT_STATUS_RES_PACKAGE,
                Contacts.CONTACT_STATUS_LABEL,
                Contacts.CONTACT_STATUS_ICON,
                GroupMembership.GROUP_SOURCE_ID,
        });
    }

    public void testRawEntityProjection() {
        assertProjection(RawContactsEntity.CONTENT_URI, new String[]{
                RawContacts.Entity.DATA_ID,
                RawContacts._ID,
                RawContacts.CONTACT_ID,
                RawContacts.ACCOUNT_NAME,
                RawContacts.ACCOUNT_TYPE,
                RawContacts.DATA_SET,
                RawContacts.ACCOUNT_TYPE_AND_DATA_SET,
                RawContacts.SOURCE_ID,
                RawContacts.VERSION,
                RawContacts.DIRTY,
                RawContacts.NAME_VERIFIED,
                RawContacts.DELETED,
                RawContacts.SYNC1,
                RawContacts.SYNC2,
                RawContacts.SYNC3,
                RawContacts.SYNC4,
                RawContacts.STARRED,
                RawContacts.RAW_CONTACT_IS_USER_PROFILE,
                Data.DATA_VERSION,
                Data.IS_PRIMARY,
                Data.IS_SUPER_PRIMARY,
                Data.RES_PACKAGE,
                Data.MIMETYPE,
                Data.DATA1,
                Data.DATA2,
                Data.DATA3,
                Data.DATA4,
                Data.DATA5,
                Data.DATA6,
                Data.DATA7,
                Data.DATA8,
                Data.DATA9,
                Data.DATA10,
                Data.DATA11,
                Data.DATA12,
                Data.DATA13,
                Data.DATA14,
                Data.DATA15,
                Data.SYNC1,
                Data.SYNC2,
                Data.SYNC3,
                Data.SYNC4,
                GroupMembership.GROUP_SOURCE_ID,
        });
    }

    public void testPhoneLookupProjection() {
        assertProjection(PhoneLookup.CONTENT_FILTER_URI.buildUpon().appendPath("123").build(),
            new String[]{
                PhoneLookup._ID,
                PhoneLookup.LOOKUP_KEY,
                PhoneLookup.DISPLAY_NAME,
                PhoneLookup.LAST_TIME_CONTACTED,
                PhoneLookup.TIMES_CONTACTED,
                PhoneLookup.STARRED,
                PhoneLookup.IN_VISIBLE_GROUP,
                PhoneLookup.PHOTO_ID,
                PhoneLookup.PHOTO_URI,
                PhoneLookup.PHOTO_THUMBNAIL_URI,
                PhoneLookup.CUSTOM_RINGTONE,
                PhoneLookup.HAS_PHONE_NUMBER,
                PhoneLookup.SEND_TO_VOICEMAIL,
                PhoneLookup.NUMBER,
                PhoneLookup.TYPE,
                PhoneLookup.LABEL,
                PhoneLookup.NORMALIZED_NUMBER,
        });
    }

    public void testGroupsProjection() {
        assertProjection(Groups.CONTENT_URI, new String[]{
                Groups._ID,
                Groups.ACCOUNT_NAME,
                Groups.ACCOUNT_TYPE,
                Groups.DATA_SET,
                Groups.ACCOUNT_TYPE_AND_DATA_SET,
                Groups.SOURCE_ID,
                Groups.DIRTY,
                Groups.VERSION,
                Groups.RES_PACKAGE,
                Groups.TITLE,
                Groups.TITLE_RES,
                Groups.GROUP_VISIBLE,
                Groups.SYSTEM_ID,
                Groups.DELETED,
                Groups.NOTES,
                Groups.SHOULD_SYNC,
                Groups.FAVORITES,
                Groups.AUTO_ADD,
                Groups.GROUP_IS_READ_ONLY,
                Groups.SYNC1,
                Groups.SYNC2,
                Groups.SYNC3,
                Groups.SYNC4,
        });
    }

    public void testGroupsSummaryProjection() {
        assertProjection(Groups.CONTENT_SUMMARY_URI, new String[]{
                Groups._ID,
                Groups.ACCOUNT_NAME,
                Groups.ACCOUNT_TYPE,
                Groups.DATA_SET,
                Groups.ACCOUNT_TYPE_AND_DATA_SET,
                Groups.SOURCE_ID,
                Groups.DIRTY,
                Groups.VERSION,
                Groups.RES_PACKAGE,
                Groups.TITLE,
                Groups.TITLE_RES,
                Groups.GROUP_VISIBLE,
                Groups.SYSTEM_ID,
                Groups.DELETED,
                Groups.NOTES,
                Groups.SHOULD_SYNC,
                Groups.FAVORITES,
                Groups.AUTO_ADD,
                Groups.GROUP_IS_READ_ONLY,
                Groups.SYNC1,
                Groups.SYNC2,
                Groups.SYNC3,
                Groups.SYNC4,
                Groups.SUMMARY_COUNT,
                Groups.SUMMARY_WITH_PHONES,
        });
    }

    public void testAggregateExceptionProjection() {
        assertProjection(AggregationExceptions.CONTENT_URI, new String[]{
                AggregationExceptionColumns._ID,
                AggregationExceptions.TYPE,
                AggregationExceptions.RAW_CONTACT_ID1,
                AggregationExceptions.RAW_CONTACT_ID2,
        });
    }

    public void testSettingsProjection() {
        assertProjection(Settings.CONTENT_URI, new String[]{
                Settings.ACCOUNT_NAME,
                Settings.ACCOUNT_TYPE,
                Settings.DATA_SET,
                Settings.UNGROUPED_VISIBLE,
                Settings.SHOULD_SYNC,
                Settings.ANY_UNSYNCED,
                Settings.UNGROUPED_COUNT,
                Settings.UNGROUPED_WITH_PHONES,
        });
    }

    public void testStatusUpdatesProjection() {
        assertProjection(StatusUpdates.CONTENT_URI, new String[]{
                PresenceColumns.RAW_CONTACT_ID,
                StatusUpdates.DATA_ID,
                StatusUpdates.IM_ACCOUNT,
                StatusUpdates.IM_HANDLE,
                StatusUpdates.PROTOCOL,
                StatusUpdates.CUSTOM_PROTOCOL,
                StatusUpdates.PRESENCE,
                StatusUpdates.CHAT_CAPABILITY,
                StatusUpdates.STATUS,
                StatusUpdates.STATUS_TIMESTAMP,
                StatusUpdates.STATUS_RES_PACKAGE,
                StatusUpdates.STATUS_ICON,
                StatusUpdates.STATUS_LABEL,
        });
    }

    public void testDirectoryProjection() {
        assertProjection(Directory.CONTENT_URI, new String[]{
                Directory._ID,
                Directory.PACKAGE_NAME,
                Directory.TYPE_RESOURCE_ID,
                Directory.DISPLAY_NAME,
                Directory.DIRECTORY_AUTHORITY,
                Directory.ACCOUNT_TYPE,
                Directory.ACCOUNT_NAME,
                Directory.EXPORT_SUPPORT,
                Directory.SHORTCUT_SUPPORT,
                Directory.PHOTO_SUPPORT,
        });
    }

    public void testRawContactsInsert() {
        ContentValues values = new ContentValues();

        values.put(RawContacts.ACCOUNT_NAME, "a");
        values.put(RawContacts.ACCOUNT_TYPE, "b");
        values.put(RawContacts.SOURCE_ID, "c");
        values.put(RawContacts.VERSION, 42);
        values.put(RawContacts.DIRTY, 1);
        values.put(RawContacts.DELETED, 1);
        values.put(RawContacts.AGGREGATION_MODE, RawContacts.AGGREGATION_MODE_DISABLED);
        values.put(RawContacts.CUSTOM_RINGTONE, "d");
        values.put(RawContacts.SEND_TO_VOICEMAIL, 1);
        values.put(RawContacts.LAST_TIME_CONTACTED, 12345);
        values.put(RawContacts.STARRED, 1);
        values.put(RawContacts.SYNC1, "e");
        values.put(RawContacts.SYNC2, "f");
        values.put(RawContacts.SYNC3, "g");
        values.put(RawContacts.SYNC4, "h");

        Uri rowUri = mResolver.insert(RawContacts.CONTENT_URI, values);
        long rawContactId = ContentUris.parseId(rowUri);

        assertStoredValues(rowUri, values);
        assertSelection(RawContacts.CONTENT_URI, values, RawContacts._ID, rawContactId);
        assertNetworkNotified(true);
    }

    public void testDataDirectoryWithLookupUri() {
        ContentValues values = new ContentValues();

        long rawContactId = createRawContactWithName();
        insertPhoneNumber(rawContactId, "555-GOOG-411");
        insertEmail(rawContactId, "google@android.com");

        long contactId = queryContactId(rawContactId);
        String lookupKey = queryLookupKey(contactId);

        // Complete and valid lookup URI
        Uri lookupUri = ContactsContract.Contacts.getLookupUri(contactId, lookupKey);
        Uri dataUri = Uri.withAppendedPath(lookupUri, Contacts.Data.CONTENT_DIRECTORY);

        assertDataRows(dataUri, values);

        // Complete but stale lookup URI
        lookupUri = ContactsContract.Contacts.getLookupUri(contactId + 1, lookupKey);
        dataUri = Uri.withAppendedPath(lookupUri, Contacts.Data.CONTENT_DIRECTORY);
        assertDataRows(dataUri, values);

        // Incomplete lookup URI (lookup key only, no contact ID)
        dataUri = Uri.withAppendedPath(Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI,
                lookupKey), Contacts.Data.CONTENT_DIRECTORY);
        assertDataRows(dataUri, values);
    }

    private void assertDataRows(Uri dataUri, ContentValues values) {
        Cursor cursor = mResolver.query(dataUri, new String[]{ Data.DATA1 }, null, null, Data._ID);
        assertEquals(3, cursor.getCount());
        cursor.moveToFirst();
        values.put(Data.DATA1, "John Doe");
        assertCursorValues(cursor, values);

        cursor.moveToNext();
        values.put(Data.DATA1, "555-GOOG-411");
        assertCursorValues(cursor, values);

        cursor.moveToNext();
        values.put(Data.DATA1, "google@android.com");
        assertCursorValues(cursor, values);

        cursor.close();
    }

    public void testContactEntitiesWithIdBasedUri() {
        ContentValues values = new ContentValues();
        Account account1 = new Account("act1", "actype1");
        Account account2 = new Account("act2", "actype2");

        long rawContactId1 = createRawContactWithName(account1);
        insertImHandle(rawContactId1, Im.PROTOCOL_GOOGLE_TALK, null, "gtalk");
        insertStatusUpdate(Im.PROTOCOL_GOOGLE_TALK, null, "gtalk", StatusUpdates.IDLE, "Busy", 90,
                StatusUpdates.CAPABILITY_HAS_CAMERA, false);

        long rawContactId2 = createRawContact(account2);
        setAggregationException(
                AggregationExceptions.TYPE_KEEP_TOGETHER, rawContactId1, rawContactId2);

        long contactId = queryContactId(rawContactId1);

        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
        Uri entityUri = Uri.withAppendedPath(contactUri, Contacts.Entity.CONTENT_DIRECTORY);

        assertEntityRows(entityUri, contactId, rawContactId1, rawContactId2);
    }

    public void testContactEntitiesWithLookupUri() {
        ContentValues values = new ContentValues();
        Account account1 = new Account("act1", "actype1");
        Account account2 = new Account("act2", "actype2");

        long rawContactId1 = createRawContactWithName(account1);
        insertImHandle(rawContactId1, Im.PROTOCOL_GOOGLE_TALK, null, "gtalk");
        insertStatusUpdate(Im.PROTOCOL_GOOGLE_TALK, null, "gtalk", StatusUpdates.IDLE, "Busy", 90,
                StatusUpdates.CAPABILITY_HAS_CAMERA, false);

        long rawContactId2 = createRawContact(account2);
        setAggregationException(
                AggregationExceptions.TYPE_KEEP_TOGETHER, rawContactId1, rawContactId2);

        long contactId = queryContactId(rawContactId1);
        String lookupKey = queryLookupKey(contactId);

        // First try with a matching contact ID
        Uri contactLookupUri = ContactsContract.Contacts.getLookupUri(contactId, lookupKey);
        Uri entityUri = Uri.withAppendedPath(contactLookupUri, Contacts.Entity.CONTENT_DIRECTORY);
        assertEntityRows(entityUri, contactId, rawContactId1, rawContactId2);

        // Now try with a contact ID mismatch
        contactLookupUri = ContactsContract.Contacts.getLookupUri(contactId + 1, lookupKey);
        entityUri = Uri.withAppendedPath(contactLookupUri, Contacts.Entity.CONTENT_DIRECTORY);
        assertEntityRows(entityUri, contactId, rawContactId1, rawContactId2);

        // Now try without an ID altogether
        contactLookupUri = Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, lookupKey);
        entityUri = Uri.withAppendedPath(contactLookupUri, Contacts.Entity.CONTENT_DIRECTORY);
        assertEntityRows(entityUri, contactId, rawContactId1, rawContactId2);
    }

    private void assertEntityRows(Uri entityUri, long contactId, long rawContactId1,
            long rawContactId2) {
        ContentValues values = new ContentValues();

        Cursor cursor = mResolver.query(entityUri, null, null, null,
                Contacts.Entity.RAW_CONTACT_ID + "," + Contacts.Entity.DATA_ID);
        assertEquals(3, cursor.getCount());

        // First row - name
        cursor.moveToFirst();
        values.put(Contacts.Entity.CONTACT_ID, contactId);
        values.put(Contacts.Entity.RAW_CONTACT_ID, rawContactId1);
        values.put(Contacts.Entity.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
        values.put(Contacts.Entity.DATA1, "John Doe");
        values.put(Contacts.Entity.ACCOUNT_NAME, "act1");
        values.put(Contacts.Entity.ACCOUNT_TYPE, "actype1");
        values.put(Contacts.Entity.DISPLAY_NAME, "John Doe");
        values.put(Contacts.Entity.DISPLAY_NAME_ALTERNATIVE, "Doe, John");
        values.put(Contacts.Entity.NAME_RAW_CONTACT_ID, rawContactId1);
        values.put(Contacts.Entity.CONTACT_CHAT_CAPABILITY, StatusUpdates.CAPABILITY_HAS_CAMERA);
        values.put(Contacts.Entity.CONTACT_PRESENCE, StatusUpdates.IDLE);
        values.put(Contacts.Entity.CONTACT_STATUS, "Busy");
        values.putNull(Contacts.Entity.PRESENCE);
        assertCursorValues(cursor, values);

        // Second row - IM
        cursor.moveToNext();
        values.put(Contacts.Entity.CONTACT_ID, contactId);
        values.put(Contacts.Entity.RAW_CONTACT_ID, rawContactId1);
        values.put(Contacts.Entity.MIMETYPE, Im.CONTENT_ITEM_TYPE);
        values.put(Contacts.Entity.DATA1, "gtalk");
        values.put(Contacts.Entity.ACCOUNT_NAME, "act1");
        values.put(Contacts.Entity.ACCOUNT_TYPE, "actype1");
        values.put(Contacts.Entity.DISPLAY_NAME, "John Doe");
        values.put(Contacts.Entity.DISPLAY_NAME_ALTERNATIVE, "Doe, John");
        values.put(Contacts.Entity.NAME_RAW_CONTACT_ID, rawContactId1);
        values.put(Contacts.Entity.CONTACT_CHAT_CAPABILITY, StatusUpdates.CAPABILITY_HAS_CAMERA);
        values.put(Contacts.Entity.CONTACT_PRESENCE, StatusUpdates.IDLE);
        values.put(Contacts.Entity.CONTACT_STATUS, "Busy");
        values.put(Contacts.Entity.PRESENCE, StatusUpdates.IDLE);
        assertCursorValues(cursor, values);

        // Third row - second raw contact, not data
        cursor.moveToNext();
        values.put(Contacts.Entity.CONTACT_ID, contactId);
        values.put(Contacts.Entity.RAW_CONTACT_ID, rawContactId2);
        values.putNull(Contacts.Entity.MIMETYPE);
        values.putNull(Contacts.Entity.DATA_ID);
        values.putNull(Contacts.Entity.DATA1);
        values.put(Contacts.Entity.ACCOUNT_NAME, "act2");
        values.put(Contacts.Entity.ACCOUNT_TYPE, "actype2");
        values.put(Contacts.Entity.DISPLAY_NAME, "John Doe");
        values.put(Contacts.Entity.DISPLAY_NAME_ALTERNATIVE, "Doe, John");
        values.put(Contacts.Entity.NAME_RAW_CONTACT_ID, rawContactId1);
        values.put(Contacts.Entity.CONTACT_CHAT_CAPABILITY, StatusUpdates.CAPABILITY_HAS_CAMERA);
        values.put(Contacts.Entity.CONTACT_PRESENCE, StatusUpdates.IDLE);
        values.put(Contacts.Entity.CONTACT_STATUS, "Busy");
        values.putNull(Contacts.Entity.PRESENCE);
        assertCursorValues(cursor, values);

        cursor.close();
    }

    public void testDataInsert() {
        long rawContactId = createRawContactWithName("John", "Doe");

        ContentValues values = new ContentValues();
        putDataValues(values, rawContactId);
        Uri dataUri = mResolver.insert(Data.CONTENT_URI, values);
        long dataId = ContentUris.parseId(dataUri);

        long contactId = queryContactId(rawContactId);
        values.put(RawContacts.CONTACT_ID, contactId);
        assertStoredValues(dataUri, values);

        assertSelection(Data.CONTENT_URI, values, Data._ID, dataId);

        // Access the same data through the directory under RawContacts
        Uri rawContactUri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId);
        Uri rawContactDataUri =
                Uri.withAppendedPath(rawContactUri, RawContacts.Data.CONTENT_DIRECTORY);
        assertSelection(rawContactDataUri, values, Data._ID, dataId);

        // Access the same data through the directory under Contacts
        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
        Uri contactDataUri = Uri.withAppendedPath(contactUri, Contacts.Data.CONTENT_DIRECTORY);
        assertSelection(contactDataUri, values, Data._ID, dataId);
        assertNetworkNotified(true);
    }

    public void testRawContactDataQuery() {
        Account account1 = new Account("a", "b");
        Account account2 = new Account("c", "d");
        long rawContactId1 = createRawContact(account1);
        Uri dataUri1 = insertStructuredName(rawContactId1, "John", "Doe");
        long rawContactId2 = createRawContact(account2);
        Uri dataUri2 = insertStructuredName(rawContactId2, "Jane", "Doe");

        Uri uri1 = maybeAddAccountQueryParameters(dataUri1, account1);
        Uri uri2 = maybeAddAccountQueryParameters(dataUri2, account2);
        assertStoredValue(uri1, Data._ID, ContentUris.parseId(dataUri1)) ;
        assertStoredValue(uri2, Data._ID, ContentUris.parseId(dataUri2)) ;
    }

    public void testPhonesQuery() {

        ContentValues values = new ContentValues();
        values.put(RawContacts.CUSTOM_RINGTONE, "d");
        values.put(RawContacts.SEND_TO_VOICEMAIL, 1);
        values.put(RawContacts.LAST_TIME_CONTACTED, 12345);
        values.put(RawContacts.TIMES_CONTACTED, 54321);
        values.put(RawContacts.STARRED, 1);

        Uri rawContactUri = mResolver.insert(RawContacts.CONTENT_URI, values);
        long rawContactId = ContentUris.parseId(rawContactUri);

        insertStructuredName(rawContactId, "Meghan", "Knox");
        Uri uri = insertPhoneNumber(rawContactId, "18004664411");
        long phoneId = ContentUris.parseId(uri);


        long contactId = queryContactId(rawContactId);
        values.clear();
        values.put(Data._ID, phoneId);
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        values.put(RawContacts.CONTACT_ID, contactId);
        values.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
        values.put(Phone.NUMBER, "18004664411");
        values.put(Phone.TYPE, Phone.TYPE_HOME);
        values.putNull(Phone.LABEL);
        values.put(Contacts.DISPLAY_NAME, "Meghan Knox");
        values.put(Contacts.CUSTOM_RINGTONE, "d");
        values.put(Contacts.SEND_TO_VOICEMAIL, 1);
        values.put(Contacts.LAST_TIME_CONTACTED, 12345);
        values.put(Contacts.TIMES_CONTACTED, 54321);
        values.put(Contacts.STARRED, 1);

        assertStoredValues(ContentUris.withAppendedId(Phone.CONTENT_URI, phoneId), values);
        assertSelection(Phone.CONTENT_URI, values, Data._ID, phoneId);
    }

    public void testPhonesWithMergedContacts() {
        long rawContactId1 = createRawContact();
        insertPhoneNumber(rawContactId1, "123456789", true);

        long rawContactId2 = createRawContact();
        insertPhoneNumber(rawContactId2, "123456789", true);

        ContentValues values1 = new ContentValues();
        values1.put(Contacts.DISPLAY_NAME, "123456789");
        values1.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
        values1.put(Phone.NUMBER, "123456789");

        // Two results should come, since they are separate entries anyway.
        assertStoredValues(Phone.CONTENT_URI, new ContentValues[] {values1, values1});

        // Even with remove_duplicate_entries flag, we should return two results here, because
        // they have different raw_contact_id-s.
        final Uri dedupeUri = Phone.CONTENT_URI.buildUpon()
                .appendQueryParameter(ContactsContract.REMOVE_DUPLICATE_ENTRIES, "true")
                .build();
        assertStoredValues(dedupeUri, new ContentValues[] {values1, values1});

        setAggregationException(AggregationExceptions.TYPE_KEEP_TOGETHER,
                rawContactId1, rawContactId2);

        assertAggregated(rawContactId1, rawContactId2, "123456789");

        // Contact merge won't affect the default result of Phone Uri.
        assertStoredValues(Phone.CONTENT_URI, new ContentValues[] {values1, values1});

        // We should detect duplicates when requested.
        assertStoredValues(dedupeUri, values1);
    }

    public void testPhonesFilterQuery() {
        long rawContactId1 = createRawContactWithName("Hot", "Tamale", ACCOUNT_1);
        insertPhoneNumber(rawContactId1, "1-800-466-4411");

        long rawContactId2 = createRawContactWithName("Chilled", "Guacamole", ACCOUNT_2);
        insertPhoneNumber(rawContactId2, "1-800-466-5432");
        insertPhoneNumber(rawContactId2, "0@example.com", false, Phone.TYPE_PAGER);
        insertPhoneNumber(rawContactId2, "1@example.com", false, Phone.TYPE_PAGER);

        Uri filterUri1 = Uri.withAppendedPath(Phone.CONTENT_FILTER_URI, "tamale");
        ContentValues values = new ContentValues();
        values.put(Contacts.DISPLAY_NAME, "Hot Tamale");
        values.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
        values.put(Phone.NUMBER, "1-800-466-4411");
        values.put(Phone.TYPE, Phone.TYPE_HOME);
        values.putNull(Phone.LABEL);
        assertStoredValuesWithProjection(filterUri1, values);

        Uri filterUri2 = Uri.withAppendedPath(Phone.CONTENT_FILTER_URI, "1-800-GOOG-411");
        assertStoredValues(filterUri2, values);

        Uri filterUri3 = Uri.withAppendedPath(Phone.CONTENT_FILTER_URI, "18004664");
        assertStoredValues(filterUri3, values);

        Uri filterUri4 = Uri.withAppendedPath(Phone.CONTENT_FILTER_URI, "encilada");
        assertEquals(0, getCount(filterUri4, null, null));

        Uri filterUri5 = Uri.withAppendedPath(Phone.CONTENT_FILTER_URI, "*");
        assertEquals(0, getCount(filterUri5, null, null));

        ContentValues values1 = new ContentValues();
        values1.put(Contacts.DISPLAY_NAME, "Chilled Guacamole");
        values1.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
        values1.put(Phone.NUMBER, "1-800-466-5432");
        values1.put(Phone.TYPE, Phone.TYPE_HOME);
        values1.putNull(Phone.LABEL);

        ContentValues values2 = new ContentValues();
        values2.put(Contacts.DISPLAY_NAME, "Chilled Guacamole");
        values2.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
        values2.put(Phone.NUMBER, "0@example.com");
        values2.put(Phone.TYPE, Phone.TYPE_PAGER);
        values2.putNull(Phone.LABEL);

        ContentValues values3 = new ContentValues();
        values3.put(Contacts.DISPLAY_NAME, "Chilled Guacamole");
        values3.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
        values3.put(Phone.NUMBER, "1@example.com");
        values3.put(Phone.TYPE, Phone.TYPE_PAGER);
        values3.putNull(Phone.LABEL);

        Uri filterUri6 = Uri.withAppendedPath(Phone.CONTENT_FILTER_URI, "Chilled");
        assertStoredValues(filterUri6, new ContentValues[] {values1, values2, values3} );
    }

    public void testPhoneLookup() {
        ContentValues values = new ContentValues();
        values.put(RawContacts.CUSTOM_RINGTONE, "d");
        values.put(RawContacts.SEND_TO_VOICEMAIL, 1);

        Uri rawContactUri = mResolver.insert(RawContacts.CONTENT_URI, values);
        long rawContactId = ContentUris.parseId(rawContactUri);

        insertStructuredName(rawContactId, "Hot", "Tamale");
        insertPhoneNumber(rawContactId, "18004664411");

        // We'll create two lookup records, 18004664411 and +18004664411, and the below lookup
        // will match both.

        Uri lookupUri1 = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, "8004664411");

        values.clear();
        values.put(PhoneLookup._ID, queryContactId(rawContactId));
        values.put(PhoneLookup.DISPLAY_NAME, "Hot Tamale");
        values.put(PhoneLookup.NUMBER, "18004664411");
        values.put(PhoneLookup.TYPE, Phone.TYPE_HOME);
        values.putNull(PhoneLookup.LABEL);
        values.put(PhoneLookup.CUSTOM_RINGTONE, "d");
        values.put(PhoneLookup.SEND_TO_VOICEMAIL, 1);
        assertStoredValues(lookupUri1, null, null, new ContentValues[] {values, values});

        // In the context that 8004664411 is a valid number, "4664411" as a
        // call id should  match to both "8004664411" and "+18004664411".
        Uri lookupUri2 = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, "4664411");
        assertEquals(2, getCount(lookupUri2, null, null));
    }

    public void testPhoneLookupUseCases() {
        ContentValues values = new ContentValues();
        Uri rawContactUri;
        long rawContactId;
        Uri lookupUri2;

        values.put(RawContacts.CUSTOM_RINGTONE, "d");
        values.put(RawContacts.SEND_TO_VOICEMAIL, 1);

        // International format in contacts
        rawContactUri = mResolver.insert(RawContacts.CONTENT_URI, values);
        rawContactId = ContentUris.parseId(rawContactUri);

        insertStructuredName(rawContactId, "Hot", "Tamale");
        insertPhoneNumber(rawContactId, "+1-650-861-0000");

        values.clear();

        // match with international format
        lookupUri2 = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, "+1 650 861 0000");
        assertEquals(1, getCount(lookupUri2, null, null));

        // match with national format
        lookupUri2 = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, "650 861 0000");
        assertEquals(1, getCount(lookupUri2, null, null));

        // National format in contacts
        values.clear();
        values.put(RawContacts.CUSTOM_RINGTONE, "d");
        values.put(RawContacts.SEND_TO_VOICEMAIL, 1);
        rawContactUri = mResolver.insert(RawContacts.CONTENT_URI, values);
        rawContactId = ContentUris.parseId(rawContactUri);

        insertStructuredName(rawContactId, "Hot1", "Tamale");
        insertPhoneNumber(rawContactId, "650-861-0001");

        values.clear();

        // match with international format
        lookupUri2 = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, "+1 650 861 0001");
        assertEquals(2, getCount(lookupUri2, null, null));

        // match with national format
        lookupUri2 = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, "650 861 0001");
        assertEquals(2, getCount(lookupUri2, null, null));

        // Local format in contacts
        values.clear();
        values.put(RawContacts.CUSTOM_RINGTONE, "d");
        values.put(RawContacts.SEND_TO_VOICEMAIL, 1);
        rawContactUri = mResolver.insert(RawContacts.CONTENT_URI, values);
        rawContactId = ContentUris.parseId(rawContactUri);

        insertStructuredName(rawContactId, "Hot2", "Tamale");
        insertPhoneNumber(rawContactId, "861-0002");

        values.clear();

        // match with international format
        lookupUri2 = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, "+1 650 861 0002");
        assertEquals(1, getCount(lookupUri2, null, null));

        // match with national format
        lookupUri2 = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, "650 861 0002");
        assertEquals(1, getCount(lookupUri2, null, null));
    }

    public void testIntlPhoneLookupUseCases() {
        // Checks the logic that relies on using the trailing 7-digits as a fallback for phone
        // number lookups.
        String fullNumber = "01197297427289";

        ContentValues values = new ContentValues();
        values.put(RawContacts.CUSTOM_RINGTONE, "d");
        values.put(RawContacts.SEND_TO_VOICEMAIL, 1);
        long rawContactId = ContentUris.parseId(mResolver.insert(RawContacts.CONTENT_URI, values));
        insertStructuredName(rawContactId, "Senor", "Chang");
        insertPhoneNumber(rawContactId, fullNumber);

        // Full number should definitely match.
        assertEquals(2, getCount(Uri.withAppendedPath(
                PhoneLookup.CONTENT_FILTER_URI, fullNumber), null, null));

        // Shorter (local) number with 0 prefix should also match.
        assertEquals(2, getCount(Uri.withAppendedPath(
                PhoneLookup.CONTENT_FILTER_URI, "097427289"), null, null));

        // Shorter (local) number with +0 prefix should also match.
        assertEquals(2, getCount(Uri.withAppendedPath(
                PhoneLookup.CONTENT_FILTER_URI, "+097427289"), null, null));

        // Same shorter number with dashes should match.
        assertEquals(2, getCount(Uri.withAppendedPath(
                PhoneLookup.CONTENT_FILTER_URI, "09-742-7289"), null, null));

        // Same shorter number with spaces should match.
        assertEquals(2, getCount(Uri.withAppendedPath(
                PhoneLookup.CONTENT_FILTER_URI, "09 742 7289"), null, null));

        // Some other number should not match.
        assertEquals(0, getCount(Uri.withAppendedPath(
                PhoneLookup.CONTENT_FILTER_URI, "049102395"), null, null));
    }

    public void testPhoneLookupB5252190() {
        // Test cases from b/5252190
        String storedNumber = "796010101";

        ContentValues values = new ContentValues();
        values.put(RawContacts.CUSTOM_RINGTONE, "d");
        values.put(RawContacts.SEND_TO_VOICEMAIL, 1);
        long rawContactId = ContentUris.parseId(mResolver.insert(RawContacts.CONTENT_URI, values));
        insertStructuredName(rawContactId, "Senor", "Chang");
        insertPhoneNumber(rawContactId, storedNumber);

        assertEquals(1, getCount(Uri.withAppendedPath(
                PhoneLookup.CONTENT_FILTER_URI, "0796010101"), null, null));

        assertEquals(1, getCount(Uri.withAppendedPath(
                PhoneLookup.CONTENT_FILTER_URI, "+48796010101"), null, null));

        assertEquals(1, getCount(Uri.withAppendedPath(
                PhoneLookup.CONTENT_FILTER_URI, "48796010101"), null, null));

        assertEquals(1, getCount(Uri.withAppendedPath(
                PhoneLookup.CONTENT_FILTER_URI, "4-879-601-0101"), null, null));

        assertEquals(1, getCount(Uri.withAppendedPath(
                PhoneLookup.CONTENT_FILTER_URI, "4 879 601 0101"), null, null));
    }

    public void testPhoneUpdate() {
        ContentValues values = new ContentValues();
        Uri rawContactUri = mResolver.insert(RawContacts.CONTENT_URI, values);
        long rawContactId = ContentUris.parseId(rawContactUri);

        insertStructuredName(rawContactId, "Hot", "Tamale");
        Uri phoneUri = insertPhoneNumber(rawContactId, "18004664411");

        Uri lookupUri1 = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, "8004664411");
        Uri lookupUri2 = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, "8004664422");
        assertEquals(2, getCount(lookupUri1, null, null));
        assertEquals(0, getCount(lookupUri2, null, null));

        values.clear();
        values.put(Phone.NUMBER, "18004664422");
        mResolver.update(phoneUri, values, null, null);

        assertEquals(0, getCount(lookupUri1, null, null));
        assertEquals(2, getCount(lookupUri2, null, null));

        // Setting number to null will remove the phone lookup record
        values.clear();
        values.putNull(Phone.NUMBER);
        mResolver.update(phoneUri, values, null, null);

        assertEquals(0, getCount(lookupUri1, null, null));
        assertEquals(0, getCount(lookupUri2, null, null));

        // Let's restore that phone lookup record
        values.clear();
        values.put(Phone.NUMBER, "18004664422");
        mResolver.update(phoneUri, values, null, null);
        assertEquals(0, getCount(lookupUri1, null, null));
        assertEquals(2, getCount(lookupUri2, null, null));
        assertNetworkNotified(true);
    }

    public void testEmailsQuery() {
        ContentValues values = new ContentValues();
        values.put(RawContacts.CUSTOM_RINGTONE, "d");
        values.put(RawContacts.SEND_TO_VOICEMAIL, 1);
        values.put(RawContacts.LAST_TIME_CONTACTED, 12345);
        values.put(RawContacts.TIMES_CONTACTED, 54321);
        values.put(RawContacts.STARRED, 1);

        Uri rawContactUri = mResolver.insert(RawContacts.CONTENT_URI, values);
        final long rawContactId = ContentUris.parseId(rawContactUri);

        insertStructuredName(rawContactId, "Meghan", "Knox");
        final Uri emailUri = insertEmail(rawContactId, "meghan@acme.com");
        final long emailId = ContentUris.parseId(emailUri);

        final long contactId = queryContactId(rawContactId);
        values.clear();
        values.put(Data._ID, emailId);
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        values.put(RawContacts.CONTACT_ID, contactId);
        values.put(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
        values.put(Email.DATA, "meghan@acme.com");
        values.put(Email.TYPE, Email.TYPE_HOME);
        values.putNull(Email.LABEL);
        values.put(Contacts.DISPLAY_NAME, "Meghan Knox");
        values.put(Contacts.CUSTOM_RINGTONE, "d");
        values.put(Contacts.SEND_TO_VOICEMAIL, 1);
        values.put(Contacts.LAST_TIME_CONTACTED, 12345);
        values.put(Contacts.TIMES_CONTACTED, 54321);
        values.put(Contacts.STARRED, 1);

        assertStoredValues(Email.CONTENT_URI, values);
        assertStoredValues(ContentUris.withAppendedId(Email.CONTENT_URI, emailId), values);
        assertSelection(Email.CONTENT_URI, values, Data._ID, emailId);

        // Check if the provider detects duplicated email addresses.
        final Uri emailUri2 = insertEmail(rawContactId, "meghan@acme.com");
        final long emailId2 = ContentUris.parseId(emailUri2);
        final ContentValues values2 = new ContentValues(values);
        values2.put(Data._ID, emailId2);

        final Uri dedupeUri = Email.CONTENT_URI.buildUpon()
                .appendQueryParameter(ContactsContract.REMOVE_DUPLICATE_ENTRIES, "true")
                .build();

        // URI with ID should return a correct result.
        assertStoredValues(ContentUris.withAppendedId(Email.CONTENT_URI, emailId), values);
        assertStoredValues(ContentUris.withAppendedId(dedupeUri, emailId), values);
        assertStoredValues(ContentUris.withAppendedId(Email.CONTENT_URI, emailId2), values2);
        assertStoredValues(ContentUris.withAppendedId(dedupeUri, emailId2), values2);

        assertStoredValues(Email.CONTENT_URI, new ContentValues[] {values, values2});

        // If requested to remove duplicates, the query should return just one result,
        // whose _ID won't be deterministic.
        values.remove(Data._ID);
        assertStoredValues(dedupeUri, values);
    }

    public void testEmailsLookupQuery() {
        long rawContactId = createRawContactWithName("Hot", "Tamale");
        insertEmail(rawContactId, "tamale@acme.com");

        Uri filterUri1 = Uri.withAppendedPath(Email.CONTENT_LOOKUP_URI, "tamale@acme.com");
        ContentValues values = new ContentValues();
        values.put(Contacts.DISPLAY_NAME, "Hot Tamale");
        values.put(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
        values.put(Email.DATA, "tamale@acme.com");
        values.put(Email.TYPE, Email.TYPE_HOME);
        values.putNull(Email.LABEL);
        assertStoredValues(filterUri1, values);

        Uri filterUri2 = Uri.withAppendedPath(Email.CONTENT_LOOKUP_URI, "Ta<TaMale@acme.com>");
        assertStoredValues(filterUri2, values);

        Uri filterUri3 = Uri.withAppendedPath(Email.CONTENT_LOOKUP_URI, "encilada@acme.com");
        assertEquals(0, getCount(filterUri3, null, null));
    }

    public void testEmailsFilterQuery() {
        long rawContactId1 = createRawContactWithName("Hot", "Tamale", ACCOUNT_1);
        insertEmail(rawContactId1, "tamale@acme.com");
        insertEmail(rawContactId1, "tamale@acme.com");

        long rawContactId2 = createRawContactWithName("Hot", "Tamale", ACCOUNT_2);
        insertEmail(rawContactId2, "tamale@acme.com");

        Uri filterUri1 = Uri.withAppendedPath(Email.CONTENT_FILTER_URI, "tam");
        ContentValues values = new ContentValues();
        values.put(Contacts.DISPLAY_NAME, "Hot Tamale");
        values.put(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
        values.put(Email.DATA, "tamale@acme.com");
        values.put(Email.TYPE, Email.TYPE_HOME);
        values.putNull(Email.LABEL);
        assertStoredValuesWithProjection(filterUri1, values);

        Uri filterUri2 = Uri.withAppendedPath(Email.CONTENT_FILTER_URI, "hot");
        assertStoredValuesWithProjection(filterUri2, values);

        Uri filterUri3 = Uri.withAppendedPath(Email.CONTENT_FILTER_URI, "hot tamale");
        assertStoredValuesWithProjection(filterUri3, values);

        Uri filterUri4 = Uri.withAppendedPath(Email.CONTENT_FILTER_URI, "tamale@acme");
        assertStoredValuesWithProjection(filterUri4, values);

        Uri filterUri5 = Uri.withAppendedPath(Email.CONTENT_FILTER_URI, "encilada");
        assertEquals(0, getCount(filterUri5, null, null));
    }

    /**
     * Tests if ContactsProvider2 returns addresses according to registration order.
     */
    public void testEmailFilterDefaultSortOrder() {
        long rawContactId1 = createRawContact();
        insertEmail(rawContactId1, "address1@email.com");
        insertEmail(rawContactId1, "address2@email.com");
        insertEmail(rawContactId1, "address3@email.com");
        ContentValues v1 = new ContentValues();
        v1.put(Email.ADDRESS, "address1@email.com");
        ContentValues v2 = new ContentValues();
        v2.put(Email.ADDRESS, "address2@email.com");
        ContentValues v3 = new ContentValues();
        v3.put(Email.ADDRESS, "address3@email.com");

        Uri filterUri = Uri.withAppendedPath(Email.CONTENT_FILTER_URI, "address");
        assertStoredValuesOrderly(filterUri, new ContentValues[] { v1, v2, v3 });
    }

    /**
     * Tests if ContactsProvider2 returns primary addresses before the other addresses.
     */
    public void testEmailFilterPrimaryAddress() {
        long rawContactId1 = createRawContact();
        insertEmail(rawContactId1, "address1@email.com");
        insertEmail(rawContactId1, "address2@email.com", true);
        ContentValues v1 = new ContentValues();
        v1.put(Email.ADDRESS, "address1@email.com");
        ContentValues v2 = new ContentValues();
        v2.put(Email.ADDRESS, "address2@email.com");

        Uri filterUri = Uri.withAppendedPath(Email.CONTENT_FILTER_URI, "address");
        assertStoredValuesOrderly(filterUri, new ContentValues[] { v2, v1 });
    }

    /**
     * Tests if ContactsProvider2 has email address associated with a primary account before the
     * other address.
     */
    public void testEmailFilterPrimaryAccount() {
        long rawContactId1 = createRawContact(ACCOUNT_1);
        insertEmail(rawContactId1, "account1@email.com");
        long rawContactId2 = createRawContact(ACCOUNT_2);
        insertEmail(rawContactId2, "account2@email.com");
        ContentValues v1 = new ContentValues();
        v1.put(Email.ADDRESS, "account1@email.com");
        ContentValues v2 = new ContentValues();
        v2.put(Email.ADDRESS, "account2@email.com");

        Uri filterUri1 = Email.CONTENT_FILTER_URI.buildUpon().appendPath("acc")
                .appendQueryParameter(ContactsContract.PRIMARY_ACCOUNT_NAME, ACCOUNT_1.name)
                .appendQueryParameter(ContactsContract.PRIMARY_ACCOUNT_TYPE, ACCOUNT_1.type)
                .build();
        assertStoredValuesOrderly(filterUri1, new ContentValues[] { v1, v2 });

        Uri filterUri2 = Email.CONTENT_FILTER_URI.buildUpon().appendPath("acc")
                .appendQueryParameter(ContactsContract.PRIMARY_ACCOUNT_NAME, ACCOUNT_2.name)
                .appendQueryParameter(ContactsContract.PRIMARY_ACCOUNT_TYPE, ACCOUNT_2.type)
                .build();
        assertStoredValuesOrderly(filterUri2, new ContentValues[] { v2, v1 });

        // Just with PRIMARY_ACCOUNT_NAME
        Uri filterUri3 = Email.CONTENT_FILTER_URI.buildUpon().appendPath("acc")
                .appendQueryParameter(ContactsContract.PRIMARY_ACCOUNT_NAME, ACCOUNT_1.name)
                .build();
        assertStoredValuesOrderly(filterUri3, new ContentValues[] { v1, v2 });

        Uri filterUri4 = Email.CONTENT_FILTER_URI.buildUpon().appendPath("acc")
                .appendQueryParameter(ContactsContract.PRIMARY_ACCOUNT_NAME, ACCOUNT_2.name)
                .build();
        assertStoredValuesOrderly(filterUri4, new ContentValues[] { v2, v1 });
    }

    /** Tests {@link DataUsageFeedback} correctly promotes a data row instead of a raw contact. */
    public void testEmailFilterSortOrderWithFeedback() {
        long rawContactId1 = createRawContact();
        String address1 = "address1@email.com";
        insertEmail(rawContactId1, address1);
        long rawContactId2 = createRawContact();
        String address2 = "address2@email.com";
        insertEmail(rawContactId2, address2);
        String address3 = "address3@email.com";
        ContentUris.parseId(insertEmail(rawContactId2, address3));

        ContentValues v1 = new ContentValues();
        v1.put(Email.ADDRESS, "address1@email.com");
        ContentValues v2 = new ContentValues();
        v2.put(Email.ADDRESS, "address2@email.com");
        ContentValues v3 = new ContentValues();
        v3.put(Email.ADDRESS, "address3@email.com");

        Uri filterUri1 = Uri.withAppendedPath(Email.CONTENT_FILTER_URI, "address");
        Uri filterUri2 = Email.CONTENT_FILTER_URI.buildUpon().appendPath("address")
                .appendQueryParameter(DataUsageFeedback.USAGE_TYPE,
                        DataUsageFeedback.USAGE_TYPE_CALL)
                .build();
        Uri filterUri3 = Email.CONTENT_FILTER_URI.buildUpon().appendPath("address")
                .appendQueryParameter(DataUsageFeedback.USAGE_TYPE,
                        DataUsageFeedback.USAGE_TYPE_LONG_TEXT)
                .build();
        Uri filterUri4 = Email.CONTENT_FILTER_URI.buildUpon().appendPath("address")
                .appendQueryParameter(DataUsageFeedback.USAGE_TYPE,
                        DataUsageFeedback.USAGE_TYPE_SHORT_TEXT)
                .build();
        assertStoredValuesOrderly(filterUri1, new ContentValues[] { v1, v2, v3 });
        assertStoredValuesOrderly(filterUri2, new ContentValues[] { v1, v2, v3 });
        assertStoredValuesOrderly(filterUri3, new ContentValues[] { v1, v2, v3 });
        assertStoredValuesOrderly(filterUri4, new ContentValues[] { v1, v2, v3 });

        sendFeedback(address3, DataUsageFeedback.USAGE_TYPE_LONG_TEXT, v3);

        // account3@email.com should be the first. account2@email.com should also be promoted as
        // it has same contact id.
        assertStoredValuesOrderly(filterUri1, new ContentValues[] { v3, v1, v2 });
        assertStoredValuesOrderly(filterUri3, new ContentValues[] { v3, v1, v2 });
    }

    /**
     * Tests {@link DataUsageFeedback} correctly bucketize contacts using each
     * {@link DataUsageStatColumns#LAST_TIME_USED}
     */
    public void testEmailFilterSortOrderWithOldHistory() {
        long rawContactId1 = createRawContact();
        long dataId1 = ContentUris.parseId(insertEmail(rawContactId1, "address1@email.com"));
        long dataId2 = ContentUris.parseId(insertEmail(rawContactId1, "address2@email.com"));
        long dataId3 = ContentUris.parseId(insertEmail(rawContactId1, "address3@email.com"));
        long dataId4 = ContentUris.parseId(insertEmail(rawContactId1, "address4@email.com"));

        Uri filterUri1 = Uri.withAppendedPath(Email.CONTENT_FILTER_URI, "address");

        ContentValues v1 = new ContentValues();
        v1.put(Email.ADDRESS, "address1@email.com");
        ContentValues v2 = new ContentValues();
        v2.put(Email.ADDRESS, "address2@email.com");
        ContentValues v3 = new ContentValues();
        v3.put(Email.ADDRESS, "address3@email.com");
        ContentValues v4 = new ContentValues();
        v4.put(Email.ADDRESS, "address4@email.com");

        final ContactsProvider2 provider = (ContactsProvider2) getProvider();

        long nowInMillis = System.currentTimeMillis();
        long yesterdayInMillis = (nowInMillis - 24 * 60 * 60 * 1000);
        long sevenDaysAgoInMillis = (nowInMillis - 7 * 24 * 60 * 60 * 1000);
        long oneYearAgoInMillis = (nowInMillis - 365L * 24 * 60 * 60 * 1000);

        // address4 is contacted just once yesterday.
        provider.updateDataUsageStat(Arrays.asList(dataId4),
                DataUsageFeedback.USAGE_TYPE_LONG_TEXT, yesterdayInMillis);

        // address3 is contacted twice 1 week ago.
        provider.updateDataUsageStat(Arrays.asList(dataId3),
                DataUsageFeedback.USAGE_TYPE_LONG_TEXT, sevenDaysAgoInMillis);
        provider.updateDataUsageStat(Arrays.asList(dataId3),
                DataUsageFeedback.USAGE_TYPE_LONG_TEXT, sevenDaysAgoInMillis);

        // address2 is contacted three times 1 year ago.
        provider.updateDataUsageStat(Arrays.asList(dataId2),
                DataUsageFeedback.USAGE_TYPE_LONG_TEXT, oneYearAgoInMillis);
        provider.updateDataUsageStat(Arrays.asList(dataId2),
                DataUsageFeedback.USAGE_TYPE_LONG_TEXT, oneYearAgoInMillis);
        provider.updateDataUsageStat(Arrays.asList(dataId2),
                DataUsageFeedback.USAGE_TYPE_LONG_TEXT, oneYearAgoInMillis);

        // auto-complete should prefer recently contacted methods
        assertStoredValuesOrderly(filterUri1, new ContentValues[] { v4, v3, v2, v1 });

        // Pretend address2 is contacted right now
        provider.updateDataUsageStat(Arrays.asList(dataId2),
                DataUsageFeedback.USAGE_TYPE_LONG_TEXT, nowInMillis);

        // Now address2 is the most recently used address
        assertStoredValuesOrderly(filterUri1, new ContentValues[] { v2, v4, v3, v1 });

        // Pretend address1 is contacted right now
        provider.updateDataUsageStat(Arrays.asList(dataId1),
                DataUsageFeedback.USAGE_TYPE_LONG_TEXT, nowInMillis);

        // address2 is preferred to address1 as address2 is used 4 times in total
        assertStoredValuesOrderly(filterUri1, new ContentValues[] { v2, v1, v4, v3 });
    }

    public void testPostalsQuery() {
        long rawContactId = createRawContactWithName("Alice", "Nextore");
        Uri dataUri = insertPostalAddress(rawContactId, "1600 Amphiteatre Ave, Mountain View");
        final long dataId = ContentUris.parseId(dataUri);

        final long contactId = queryContactId(rawContactId);
        ContentValues values = new ContentValues();
        values.put(Data._ID, dataId);
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        values.put(RawContacts.CONTACT_ID, contactId);
        values.put(Data.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE);
        values.put(StructuredPostal.FORMATTED_ADDRESS, "1600 Amphiteatre Ave, Mountain View");
        values.put(Contacts.DISPLAY_NAME, "Alice Nextore");

        assertStoredValues(StructuredPostal.CONTENT_URI, values);
        assertStoredValues(ContentUris.withAppendedId(StructuredPostal.CONTENT_URI, dataId),
                values);
        assertSelection(StructuredPostal.CONTENT_URI, values, Data._ID, dataId);

        // Check if the provider detects duplicated addresses.
        Uri dataUri2 = insertPostalAddress(rawContactId, "1600 Amphiteatre Ave, Mountain View");
        final long dataId2 = ContentUris.parseId(dataUri2);
        final ContentValues values2 = new ContentValues(values);
        values2.put(Data._ID, dataId2);

        final Uri dedupeUri = StructuredPostal.CONTENT_URI.buildUpon()
                .appendQueryParameter(ContactsContract.REMOVE_DUPLICATE_ENTRIES, "true")
                .build();

        // URI with ID should return a correct result.
        assertStoredValues(ContentUris.withAppendedId(StructuredPostal.CONTENT_URI, dataId),
                values);
        assertStoredValues(ContentUris.withAppendedId(dedupeUri, dataId), values);
        assertStoredValues(ContentUris.withAppendedId(StructuredPostal.CONTENT_URI, dataId2),
                values2);
        assertStoredValues(ContentUris.withAppendedId(dedupeUri, dataId2), values2);

        assertStoredValues(StructuredPostal.CONTENT_URI, new ContentValues[] {values, values2});

        // If requested to remove duplicates, the query should return just one result,
        // whose _ID won't be deterministic.
        values.remove(Data._ID);
        assertStoredValues(dedupeUri, values);
    }

    public void testQueryContactData() {
        ContentValues values = new ContentValues();
        long contactId = createContact(values, "John", "Doe",
                "18004664411", "goog411@acme.com", StatusUpdates.INVISIBLE, 4, 1, 0,
                StatusUpdates.CAPABILITY_HAS_CAMERA | StatusUpdates.CAPABILITY_HAS_VIDEO);
        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);

        assertStoredValues(contactUri, values);
        assertSelection(Contacts.CONTENT_URI, values, Contacts._ID, contactId);
    }

    public void testQueryContactWithStatusUpdate() {
        ContentValues values = new ContentValues();
        long contactId = createContact(values, "John", "Doe",
                "18004664411", "goog411@acme.com", StatusUpdates.INVISIBLE, 4, 1, 0,
                StatusUpdates.CAPABILITY_HAS_CAMERA);
        values.put(Contacts.CONTACT_PRESENCE, StatusUpdates.INVISIBLE);
        values.put(Contacts.CONTACT_CHAT_CAPABILITY, StatusUpdates.CAPABILITY_HAS_CAMERA);
        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
        assertStoredValuesWithProjection(contactUri, values);
        assertSelectionWithProjection(Contacts.CONTENT_URI, values, Contacts._ID, contactId);
    }

    public void testQueryContactFilterByName() {
        ContentValues values = new ContentValues();
        long rawContactId = createRawContact(values, "18004664411",
                "goog411@acme.com", StatusUpdates.INVISIBLE, 4, 1, 0,
                StatusUpdates.CAPABILITY_HAS_CAMERA | StatusUpdates.CAPABILITY_HAS_VIDEO |
                StatusUpdates.CAPABILITY_HAS_VOICE);

        ContentValues nameValues = new ContentValues();
        nameValues.put(StructuredName.GIVEN_NAME, "Stu");
        nameValues.put(StructuredName.FAMILY_NAME, "Goulash");
        nameValues.put(StructuredName.PHONETIC_FAMILY_NAME, "goo");
        nameValues.put(StructuredName.PHONETIC_GIVEN_NAME, "LASH");
        Uri nameUri = insertStructuredName(rawContactId, nameValues);

        long contactId = queryContactId(rawContactId);
        values.put(Contacts.CONTACT_PRESENCE, StatusUpdates.INVISIBLE);

        Uri filterUri1 = Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI, "goulash");
        assertStoredValuesWithProjection(filterUri1, values);

        assertContactFilter(contactId, "goolash");
        assertContactFilter(contactId, "lash");

        assertContactFilterNoResult("goolish");

        // Phonetic name with given/family reversed should not match
        assertContactFilterNoResult("lashgoo");

        nameValues.clear();
        nameValues.put(StructuredName.PHONETIC_FAMILY_NAME, "ga");
        nameValues.put(StructuredName.PHONETIC_GIVEN_NAME, "losh");

        mResolver.update(nameUri, nameValues, null, null);

        assertContactFilter(contactId, "galosh");

        assertContactFilterNoResult("goolish");
    }

    public void testQueryContactFilterByEmailAddress() {
        ContentValues values = new ContentValues();
        long rawContactId = createRawContact(values, "18004664411",
                "goog411@acme.com", StatusUpdates.INVISIBLE, 4, 1, 0,
                StatusUpdates.CAPABILITY_HAS_CAMERA | StatusUpdates.CAPABILITY_HAS_VIDEO |
                StatusUpdates.CAPABILITY_HAS_VOICE);

        insertStructuredName(rawContactId, "James", "Bond");

        long contactId = queryContactId(rawContactId);
        values.put(Contacts.CONTACT_PRESENCE, StatusUpdates.INVISIBLE);

        Uri filterUri1 = Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI, "goog411@acme.com");
        assertStoredValuesWithProjection(filterUri1, values);

        assertContactFilter(contactId, "goog");
        assertContactFilter(contactId, "goog411");
        assertContactFilter(contactId, "goog411@");
        assertContactFilter(contactId, "goog411@acme");
        assertContactFilter(contactId, "goog411@acme.com");

        assertContactFilterNoResult("goog411@acme.combo");
        assertContactFilterNoResult("goog411@le.com");
        assertContactFilterNoResult("goolish");
    }

    public void testQueryContactFilterByPhoneNumber() {
        ContentValues values = new ContentValues();
        long rawContactId = createRawContact(values, "18004664411",
                "goog411@acme.com", StatusUpdates.INVISIBLE, 4, 1, 0,
                StatusUpdates.CAPABILITY_HAS_CAMERA | StatusUpdates.CAPABILITY_HAS_VIDEO |
                StatusUpdates.CAPABILITY_HAS_VOICE);

        insertStructuredName(rawContactId, "James", "Bond");

        long contactId = queryContactId(rawContactId);
        values.put(Contacts.CONTACT_PRESENCE, StatusUpdates.INVISIBLE);

        Uri filterUri1 = Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI, "18004664411");
        assertStoredValuesWithProjection(filterUri1, values);

        assertContactFilter(contactId, "18004664411");
        assertContactFilter(contactId, "1800466");
        assertContactFilter(contactId, "+18004664411");
        assertContactFilter(contactId, "8004664411");

        assertContactFilterNoResult("78004664411");
        assertContactFilterNoResult("18004664412");
        assertContactFilterNoResult("8884664411");
    }

    /**
     * Checks ContactsProvider2 works well with strequent Uris. The provider should return starred
     * contacts and frequently used contacts.
     */
    public void testQueryContactStrequent() {
        ContentValues values1 = new ContentValues();
        final String email1 = "a@acme.com";
        final int timesContacted1 = 0;
        createContact(values1, "Noah", "Tever", "18004664411",
                email1, StatusUpdates.OFFLINE, timesContacted1, 0, 0,
                StatusUpdates.CAPABILITY_HAS_CAMERA | StatusUpdates.CAPABILITY_HAS_VIDEO);
        final String phoneNumber2 = "18004664412";
        ContentValues values2 = new ContentValues();
        createContact(values2, "Sam", "Times", phoneNumber2,
                "b@acme.com", StatusUpdates.INVISIBLE, 3, 0, 0,
                StatusUpdates.CAPABILITY_HAS_CAMERA);
        ContentValues values3 = new ContentValues();
        final String phoneNumber3 = "18004664413";
        final int timesContacted3 = 5;
        createContact(values3, "Lotta", "Calling", phoneNumber3,
                "c@acme.com", StatusUpdates.AWAY, timesContacted3, 0, 0,
                StatusUpdates.CAPABILITY_HAS_VIDEO);
        ContentValues values4 = new ContentValues();
        final long rawContactId4 = createRawContact(values4, "Fay", "Veritt", null,
                "d@acme.com", StatusUpdates.AVAILABLE, 0, 1, 0,
                StatusUpdates.CAPABILITY_HAS_VIDEO | StatusUpdates.CAPABILITY_HAS_VOICE);

        // Starred contacts should be returned. TIMES_CONTACTED should be ignored and only data
        // usage feedback should be used for "frequently contacted" listing.
        assertStoredValues(Contacts.CONTENT_STREQUENT_URI, values4);

        // Send feedback for the 3rd phone number, pretending we called that person via phone.
        sendFeedback(phoneNumber3, DataUsageFeedback.USAGE_TYPE_CALL, values3);

        // After the feedback, 3rd contact should be shown after starred one.
        assertStoredValuesOrderly(Contacts.CONTENT_STREQUENT_URI,
                new ContentValues[] { values4, values3 });

        sendFeedback(email1, DataUsageFeedback.USAGE_TYPE_LONG_TEXT, values1);
        // Twice.
        sendFeedback(email1, DataUsageFeedback.USAGE_TYPE_LONG_TEXT, values1);

        // After the feedback, 1st and 3rd contacts should be shown after starred one.
        assertStoredValuesOrderly(Contacts.CONTENT_STREQUENT_URI,
                new ContentValues[] { values4, values1, values3 });

        // With phone-only parameter, 1st and 4th contacts shouldn't be returned because:
        // 1st: feedbacks are only about email, not about phone call.
        // 4th: it has no phone number though starred.
        Uri phoneOnlyStrequentUri = Contacts.CONTENT_STREQUENT_URI.buildUpon()
                .appendQueryParameter(ContactsContract.STREQUENT_PHONE_ONLY, "true")
                .build();
        assertStoredValuesOrderly(phoneOnlyStrequentUri, new ContentValues[] { values3 });

        // Now the 4th contact has a phone number.
        insertPhoneNumber(rawContactId4, "18004664414");

        // Phone only strequent should return 4th contact.
        assertStoredValuesOrderly(phoneOnlyStrequentUri, new ContentValues[] { values4, values3 });

        // Send feedback for the 2rd phone number, pretending we send the person a SMS message.
        sendFeedback(phoneNumber2, DataUsageFeedback.USAGE_TYPE_SHORT_TEXT, values1);

        // SMS feedback shouldn't affect phone-only results.
        assertStoredValuesOrderly(phoneOnlyStrequentUri, new ContentValues[] { values4, values3 });

        Uri filterUri = Uri.withAppendedPath(Contacts.CONTENT_STREQUENT_FILTER_URI, "fay");
        assertStoredValues(filterUri, values4);
    }

    /**
     * Checks ContactsProvider2 works well with frequent Uri. The provider should return frequently
     * contacted person ordered by number of times contacted.
     */
    public void testQueryContactFrequent() {
        ContentValues values1 = new ContentValues();
        final String email1 = "a@acme.com";
        createContact(values1, "Noah", "Tever", "18004664411",
                email1, StatusUpdates.OFFLINE, 0, 0, 0, 0);
        ContentValues values2 = new ContentValues();
        final String email2 = "b@acme.com";
        createContact(values2, "Sam", "Times", "18004664412",
                email2, StatusUpdates.INVISIBLE, 0, 0, 0, 0);
        ContentValues values3 = new ContentValues();
        final String phoneNumber3 = "18004664413";
        final long contactId3 = createContact(values3, "Lotta", "Calling", phoneNumber3,
                "c@acme.com", StatusUpdates.AWAY, 0, 1, 0, 0);
        ContentValues values4 = new ContentValues();
        createContact(values4, "Fay", "Veritt", "18004664414",
                "d@acme.com", StatusUpdates.AVAILABLE, 0, 1, 0, 0);

        sendFeedback(email1, DataUsageFeedback.USAGE_TYPE_LONG_TEXT, values1);

        assertStoredValues(Contacts.CONTENT_FREQUENT_URI, values1);

        // Pretend email was sent to the address twice.
        sendFeedback(email2, DataUsageFeedback.USAGE_TYPE_LONG_TEXT, values2);
        sendFeedback(email2, DataUsageFeedback.USAGE_TYPE_LONG_TEXT, values2);

        assertStoredValues(Contacts.CONTENT_FREQUENT_URI, new ContentValues[] {values2, values1});

        // Three times
        sendFeedback(phoneNumber3, DataUsageFeedback.USAGE_TYPE_CALL, values3);
        sendFeedback(phoneNumber3, DataUsageFeedback.USAGE_TYPE_CALL, values3);
        sendFeedback(phoneNumber3, DataUsageFeedback.USAGE_TYPE_CALL, values3);

        assertStoredValues(Contacts.CONTENT_FREQUENT_URI,
                new ContentValues[] {values3, values2, values1});

        // Test it works with selection/selectionArgs
        assertStoredValues(Contacts.CONTENT_FREQUENT_URI,
                Contacts.STARRED + "=?", new String[] {"0"},
                new ContentValues[] {values2, values1});
        assertStoredValues(Contacts.CONTENT_FREQUENT_URI,
                Contacts.STARRED + "=?", new String[] {"1"},
                new ContentValues[] {values3});

        values3.put(Contacts.STARRED, 0);
        assertEquals(1,
                mResolver.update(Uri.withAppendedPath(Contacts.CONTENT_URI,
                        String.valueOf(contactId3)),
                values3, null, null));
        assertStoredValues(Contacts.CONTENT_FREQUENT_URI,
                Contacts.STARRED + "=?", new String[] {"0"},
                new ContentValues[] {values3, values2, values1});
        assertStoredValues(Contacts.CONTENT_FREQUENT_URI,
                Contacts.STARRED + "=?", new String[] {"1"},
                new ContentValues[] {});
    }

    public void testQueryContactGroup() {
        long groupId = createGroup(null, "testGroup", "Test Group");

        ContentValues values1 = new ContentValues();
        createContact(values1, "Best", "West", "18004664411",
                "west@acme.com", StatusUpdates.OFFLINE, 0, 0, groupId,
                StatusUpdates.CAPABILITY_HAS_CAMERA);

        ContentValues values2 = new ContentValues();
        createContact(values2, "Rest", "East", "18004664422",
                "east@acme.com", StatusUpdates.AVAILABLE, 0, 0, 0,
                StatusUpdates.CAPABILITY_HAS_VOICE);

        Uri filterUri1 = Uri.withAppendedPath(Contacts.CONTENT_GROUP_URI, "Test Group");
        Cursor c = mResolver.query(filterUri1, null, null, null, Contacts._ID);
        assertEquals(1, c.getCount());
        c.moveToFirst();
        assertCursorValues(c, values1);
        c.close();

        Uri filterUri2 = Uri.withAppendedPath(Contacts.CONTENT_GROUP_URI, "Test Group");
        c = mResolver.query(filterUri2, null, Contacts.DISPLAY_NAME + "=?",
                new String[] { "Best West" }, Contacts._ID);
        assertEquals(1, c.getCount());
        c.close();

        Uri filterUri3 = Uri.withAppendedPath(Contacts.CONTENT_GROUP_URI, "Next Group");
        c = mResolver.query(filterUri3, null, null, null, Contacts._ID);
        assertEquals(0, c.getCount());
        c.close();
    }

    private void expectSecurityException(String failureMessage, Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        Cursor c = null;
        try {
            c = mResolver.query(uri, projection, selection, selectionArgs, sortOrder);
            fail(failureMessage);
        } catch (SecurityException expected) {
            // The security exception is expected to occur because we're missing a permission.
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public void testQueryProfileRequiresReadPermission() {
        mActor.removePermissions("android.permission.READ_PROFILE");

        createBasicProfileContact(new ContentValues());

        // Case 1: Retrieving profile contact.
        expectSecurityException(
                "Querying for the profile without READ_PROFILE access should fail.",
                Profile.CONTENT_URI, null, null, null, Contacts._ID);

        // Case 2: Retrieving profile data.
        expectSecurityException(
                "Querying for the profile data without READ_PROFILE access should fail.",
                Profile.CONTENT_URI.buildUpon().appendPath("data").build(),
                null, null, null, Contacts._ID);

        // Case 3: Retrieving profile entities.
        expectSecurityException(
                "Querying for the profile entities without READ_PROFILE access should fail.",
                Profile.CONTENT_URI.buildUpon()
                        .appendPath("entities").build(), null, null, null, Contacts._ID);
    }

    public void testQueryProfileByContactIdRequiresReadPermission() {
        long profileRawContactId = createBasicProfileContact(new ContentValues());
        long profileContactId = queryContactId(profileRawContactId);

        mActor.removePermissions("android.permission.READ_PROFILE");

        // A query for the profile contact by ID should fail.
        expectSecurityException(
                "Querying for the profile by contact ID without READ_PROFILE access should fail.",
                ContentUris.withAppendedId(Contacts.CONTENT_URI, profileContactId),
                null, null, null, Contacts._ID);
    }

    public void testQueryProfileByRawContactIdRequiresReadPermission() {
        long profileRawContactId = createBasicProfileContact(new ContentValues());

        // Remove profile read permission and attempt to retrieve the raw contact.
        mActor.removePermissions("android.permission.READ_PROFILE");
        expectSecurityException(
                "Querying for the raw contact profile without READ_PROFILE access should fail.",
                ContentUris.withAppendedId(RawContacts.CONTENT_URI,
                        profileRawContactId), null, null, null, RawContacts._ID);
    }

    public void testQueryProfileRawContactRequiresReadPermission() {
        long profileRawContactId = createBasicProfileContact(new ContentValues());

        // Remove profile read permission and attempt to retrieve the profile's raw contact data.
        mActor.removePermissions("android.permission.READ_PROFILE");

        // Case 1: Retrieve the overall raw contact set for the profile.
        expectSecurityException(
                "Querying for the raw contact profile without READ_PROFILE access should fail.",
                Profile.CONTENT_RAW_CONTACTS_URI, null, null, null, null);

        // Case 2: Retrieve the raw contact profile data for the inserted raw contact ID.
        expectSecurityException(
                "Querying for the raw profile data without READ_PROFILE access should fail.",
                ContentUris.withAppendedId(
                        Profile.CONTENT_RAW_CONTACTS_URI, profileRawContactId).buildUpon()
                        .appendPath("data").build(), null, null, null, null);

        // Case 3: Retrieve the raw contact profile entity for the inserted raw contact ID.
        expectSecurityException(
                "Querying for the raw profile entities without READ_PROFILE access should fail.",
                ContentUris.withAppendedId(
                        Profile.CONTENT_RAW_CONTACTS_URI, profileRawContactId).buildUpon()
                        .appendPath("entity").build(), null, null, null, null);
    }

    public void testQueryProfileDataByDataIdRequiresReadPermission() {
        createBasicProfileContact(new ContentValues());
        Cursor c = mResolver.query(Profile.CONTENT_URI.buildUpon().appendPath("data").build(),
                new String[]{Data._ID, Data.MIMETYPE}, null, null, null);
        assertEquals(4, c.getCount());  // Photo, phone, email, name.
        c.moveToFirst();
        long profileDataId = c.getLong(0);
        c.close();

        // Remove profile read permission and attempt to retrieve the data
        mActor.removePermissions("android.permission.READ_PROFILE");
        expectSecurityException(
                "Querying for the data in the profile without READ_PROFILE access should fail.",
                ContentUris.withAppendedId(Data.CONTENT_URI, profileDataId),
                null, null, null, null);
    }

    public void testQueryProfileDataRequiresReadPermission() {
        createBasicProfileContact(new ContentValues());

        // Remove profile read permission and attempt to retrieve all profile data.
        mActor.removePermissions("android.permission.READ_PROFILE");
        expectSecurityException(
                "Querying for the data in the profile without READ_PROFILE access should fail.",
                Profile.CONTENT_URI.buildUpon().appendPath("data").build(),
                null, null, null, null);
    }

    public void testInsertProfileRequiresWritePermission() {
        mActor.removePermissions("android.permission.WRITE_PROFILE");

        // Creating a non-profile contact should be fine.
        createBasicNonProfileContact(new ContentValues());

        // Creating a profile contact should throw an exception.
        try {
            createBasicProfileContact(new ContentValues());
            fail("Creating a profile contact should fail without WRITE_PROFILE access.");
        } catch (SecurityException expected) {
        }
    }

    public void testInsertProfileDataRequiresWritePermission() {
        long profileRawContactId = createBasicProfileContact(new ContentValues());

        mActor.removePermissions("android.permission.WRITE_PROFILE");
        try {
            insertEmail(profileRawContactId, "foo@bar.net", false);
            fail("Inserting data into a profile contact should fail without WRITE_PROFILE access.");
        } catch (SecurityException expected) {
        }
    }

    public void testUpdateDataDoesNotRequireProfilePermission() {
        mActor.removePermissions("android.permission.READ_PROFILE");
        mActor.removePermissions("android.permission.WRITE_PROFILE");

        // Create a non-profile contact.
        long rawContactId = createRawContactWithName("Domo", "Arigato");
        long dataId = getStoredLongValue(Data.CONTENT_URI,
                Data.RAW_CONTACT_ID + "=? AND " + Data.MIMETYPE + "=?",
                new String[]{String.valueOf(rawContactId), StructuredName.CONTENT_ITEM_TYPE},
                Data._ID);

        // Updates its name using a selection.
        ContentValues values = new ContentValues();
        values.put(StructuredName.GIVEN_NAME, "Bob");
        values.put(StructuredName.FAMILY_NAME, "Blob");
        mResolver.update(Data.CONTENT_URI, values, Data._ID + "=?",
                new String[]{String.valueOf(dataId)});

        // Check that the update went through.
        assertStoredValues(ContentUris.withAppendedId(Data.CONTENT_URI, dataId), values);
    }

    public void testQueryContactThenProfile() {
        ContentValues profileValues = new ContentValues();
        long profileRawContactId = createBasicProfileContact(profileValues);
        long profileContactId = queryContactId(profileRawContactId);

        ContentValues nonProfileValues = new ContentValues();
        long nonProfileRawContactId = createBasicNonProfileContact(nonProfileValues);
        long nonProfileContactId = queryContactId(nonProfileRawContactId);

        assertStoredValues(Contacts.CONTENT_URI, nonProfileValues);
        assertSelection(Contacts.CONTENT_URI, nonProfileValues, Contacts._ID, nonProfileContactId);

        assertStoredValues(Profile.CONTENT_URI, profileValues);
    }

    public void testQueryContactExcludeProfile() {
        // Create a profile contact (it should not be returned by the general contact URI).
        createBasicProfileContact(new ContentValues());

        // Create a non-profile contact - this should be returned.
        ContentValues nonProfileValues = new ContentValues();
        createBasicNonProfileContact(nonProfileValues);

        assertStoredValues(Contacts.CONTENT_URI, new ContentValues[] {nonProfileValues});
    }

    public void testQueryProfile() {
        ContentValues profileValues = new ContentValues();
        createBasicProfileContact(profileValues);

        assertStoredValues(Profile.CONTENT_URI, profileValues);
    }

    private ContentValues[] getExpectedProfileDataValues() {
        // Expected photo data values (only field is the photo BLOB, which we can't check).
        ContentValues photoRow = new ContentValues();
        photoRow.put(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);

        // Expected phone data values.
        ContentValues phoneRow = new ContentValues();
        phoneRow.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
        phoneRow.put(Phone.NUMBER, "18005554411");

        // Expected email data values.
        ContentValues emailRow = new ContentValues();
        emailRow.put(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
        emailRow.put(Email.ADDRESS, "mia.prophyl@acme.com");

        // Expected name data values.
        ContentValues nameRow = new ContentValues();
        nameRow.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
        nameRow.put(StructuredName.DISPLAY_NAME, "Mia Prophyl");
        nameRow.put(StructuredName.GIVEN_NAME, "Mia");
        nameRow.put(StructuredName.FAMILY_NAME, "Prophyl");

        return new ContentValues[]{photoRow, phoneRow, emailRow, nameRow};
    }

    public void testQueryProfileData() {
        createBasicProfileContact(new ContentValues());

        assertStoredValues(Profile.CONTENT_URI.buildUpon().appendPath("data").build(),
                getExpectedProfileDataValues());
    }

    public void testQueryProfileEntities() {
        createBasicProfileContact(new ContentValues());

        assertStoredValues(Profile.CONTENT_URI.buildUpon().appendPath("entities").build(),
                getExpectedProfileDataValues());
    }

    public void testQueryRawProfile() {
        ContentValues profileValues = new ContentValues();
        createBasicProfileContact(profileValues);

        // The raw contact view doesn't include the photo ID.
        profileValues.remove(Contacts.PHOTO_ID);
        assertStoredValues(Profile.CONTENT_RAW_CONTACTS_URI, profileValues);
    }

    public void testQueryRawProfileById() {
        ContentValues profileValues = new ContentValues();
        long profileRawContactId = createBasicProfileContact(profileValues);

        // The raw contact view doesn't include the photo ID.
        profileValues.remove(Contacts.PHOTO_ID);
        assertStoredValues(ContentUris.withAppendedId(
                Profile.CONTENT_RAW_CONTACTS_URI, profileRawContactId), profileValues);
    }

    public void testQueryRawProfileData() {
        long profileRawContactId = createBasicProfileContact(new ContentValues());

        assertStoredValues(ContentUris.withAppendedId(
                Profile.CONTENT_RAW_CONTACTS_URI, profileRawContactId).buildUpon()
                .appendPath("data").build(), getExpectedProfileDataValues());
    }

    public void testQueryRawProfileEntity() {
        long profileRawContactId = createBasicProfileContact(new ContentValues());

        assertStoredValues(ContentUris.withAppendedId(
                Profile.CONTENT_RAW_CONTACTS_URI, profileRawContactId).buildUpon()
                .appendPath("entity").build(), getExpectedProfileDataValues());
    }

    public void testQueryDataForProfile() {
        createBasicProfileContact(new ContentValues());

        assertStoredValues(Profile.CONTENT_URI.buildUpon().appendPath("data").build(),
                getExpectedProfileDataValues());
    }

    public void testUpdateProfileRawContact() {
        createBasicProfileContact(new ContentValues());
        ContentValues updatedValues = new ContentValues();
        updatedValues.put(RawContacts.SEND_TO_VOICEMAIL, 0);
        updatedValues.put(RawContacts.CUSTOM_RINGTONE, "rachmaninoff3");
        updatedValues.put(RawContacts.STARRED, 1);
        mResolver.update(Profile.CONTENT_RAW_CONTACTS_URI, updatedValues, null, null);

        assertStoredValues(Profile.CONTENT_RAW_CONTACTS_URI, updatedValues);
    }

    public void testInsertProfileWithDataSetTriggersAccountCreation() {
        // Check that we have no profile raw contacts.
        assertStoredValues(Profile.CONTENT_RAW_CONTACTS_URI, new ContentValues[]{});

        // Insert a profile record with a new data set.
        Account account = new Account("a", "b");
        String dataSet = "c";
        Uri profileUri = maybeAddAccountQueryParameters(Profile.CONTENT_RAW_CONTACTS_URI, account)
                .buildUpon().appendQueryParameter(RawContacts.DATA_SET, dataSet).build();
        ContentValues values = new ContentValues();
        long rawContactId = ContentUris.parseId(mResolver.insert(profileUri, values));
        values.put(RawContacts._ID, rawContactId);

        // Check that querying for the profile gets the created raw contact.
        assertStoredValues(Profile.CONTENT_RAW_CONTACTS_URI, values);
    }

    public void testLoadProfilePhoto() throws IOException {
        long rawContactId = createBasicProfileContact(new ContentValues());
        insertPhoto(rawContactId, R.drawable.earth_normal);
        assertInputStreamContent(
                loadPhotoFromResource(R.drawable.earth_normal, PhotoSize.THUMBNAIL),
                Contacts.openContactPhotoInputStream(mResolver, Profile.CONTENT_URI, false));
    }

    public void testLoadProfileDisplayPhoto() throws IOException {
        long rawContactId = createBasicProfileContact(new ContentValues());
        insertPhoto(rawContactId, R.drawable.earth_normal);
        assertInputStreamContent(
                loadPhotoFromResource(R.drawable.earth_normal, PhotoSize.DISPLAY_PHOTO),
                Contacts.openContactPhotoInputStream(mResolver, Profile.CONTENT_URI, true));
    }

    public void testPhonesWithStatusUpdate() {

        ContentValues values = new ContentValues();
        Uri rawContactUri = mResolver.insert(RawContacts.CONTENT_URI, values);
        long rawContactId = ContentUris.parseId(rawContactUri);
        insertStructuredName(rawContactId, "John", "Doe");
        Uri photoUri = insertPhoto(rawContactId);
        long photoId = ContentUris.parseId(photoUri);
        insertPhoneNumber(rawContactId, "18004664411");
        insertPhoneNumber(rawContactId, "18004664412");
        insertEmail(rawContactId, "goog411@acme.com");
        insertEmail(rawContactId, "goog412@acme.com");

        insertStatusUpdate(Im.PROTOCOL_GOOGLE_TALK, null, "goog411@acme.com",
                StatusUpdates.INVISIBLE, "Bad",
                StatusUpdates.CAPABILITY_HAS_CAMERA);
        insertStatusUpdate(Im.PROTOCOL_GOOGLE_TALK, null, "goog412@acme.com",
                StatusUpdates.AVAILABLE, "Good",
                StatusUpdates.CAPABILITY_HAS_CAMERA | StatusUpdates.CAPABILITY_HAS_VOICE);
        long contactId = queryContactId(rawContactId);

        Uri uri = Data.CONTENT_URI;

        Cursor c = mResolver.query(uri, null, RawContacts.CONTACT_ID + "=" + contactId + " AND "
                + Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'", null, Phone.NUMBER);
        assertEquals(2, c.getCount());

        c.moveToFirst();

        values.clear();
        values.put(Contacts.CONTACT_PRESENCE, StatusUpdates.AVAILABLE);
        values.put(Contacts.CONTACT_STATUS, "Bad");
        values.put(Contacts.DISPLAY_NAME, "John Doe");
        values.put(Phone.NUMBER, "18004664411");
        values.putNull(Phone.LABEL);
        values.put(RawContacts.CONTACT_ID, contactId);
        assertCursorValues(c, values);

        c.moveToNext();

        values.clear();
        values.put(Contacts.CONTACT_PRESENCE, StatusUpdates.AVAILABLE);
        values.put(Contacts.CONTACT_STATUS, "Bad");
        values.put(Contacts.DISPLAY_NAME, "John Doe");
        values.put(Phone.NUMBER, "18004664412");
        values.putNull(Phone.LABEL);
        values.put(RawContacts.CONTACT_ID, contactId);
        assertCursorValues(c, values);

        c.close();
    }

    public void testGroupQuery() {
        Account account1 = new Account("a", "b");
        Account account2 = new Account("c", "d");
        long groupId1 = createGroup(account1, "e", "f");
        long groupId2 = createGroup(account2, "g", "h");
        Uri uri1 = maybeAddAccountQueryParameters(Groups.CONTENT_URI, account1);
        Uri uri2 = maybeAddAccountQueryParameters(Groups.CONTENT_URI, account2);
        assertEquals(1, getCount(uri1, null, null));
        assertEquals(1, getCount(uri2, null, null));
        assertStoredValue(uri1, Groups._ID + "=" + groupId1, null, Groups._ID, groupId1) ;
        assertStoredValue(uri2, Groups._ID + "=" + groupId2, null, Groups._ID, groupId2) ;
    }

    public void testGroupInsert() {
        ContentValues values = new ContentValues();

        values.put(Groups.ACCOUNT_NAME, "a");
        values.put(Groups.ACCOUNT_TYPE, "b");
        values.put(Groups.SOURCE_ID, "c");
        values.put(Groups.VERSION, 42);
        values.put(Groups.GROUP_VISIBLE, 1);
        values.put(Groups.TITLE, "d");
        values.put(Groups.TITLE_RES, 1234);
        values.put(Groups.NOTES, "e");
        values.put(Groups.RES_PACKAGE, "f");
        values.put(Groups.SYSTEM_ID, "g");
        values.put(Groups.DELETED, 1);
        values.put(Groups.SYNC1, "h");
        values.put(Groups.SYNC2, "i");
        values.put(Groups.SYNC3, "j");
        values.put(Groups.SYNC4, "k");

        Uri rowUri = mResolver.insert(Groups.CONTENT_URI, values);

        values.put(Groups.DIRTY, 1);
        assertStoredValues(rowUri, values);
    }

    public void testGroupCreationAfterMembershipInsert() {
        long rawContactId1 = createRawContact(mAccount);
        Uri groupMembershipUri = insertGroupMembership(rawContactId1, "gsid1");

        long groupId = assertSingleGroup(NO_LONG, mAccount, "gsid1", null);
        assertSingleGroupMembership(ContentUris.parseId(groupMembershipUri),
                rawContactId1, groupId, "gsid1");
    }

    public void testGroupReuseAfterMembershipInsert() {
        long rawContactId1 = createRawContact(mAccount);
        long groupId1 = createGroup(mAccount, "gsid1", "title1");
        Uri groupMembershipUri = insertGroupMembership(rawContactId1, "gsid1");

        assertSingleGroup(groupId1, mAccount, "gsid1", "title1");
        assertSingleGroupMembership(ContentUris.parseId(groupMembershipUri),
                rawContactId1, groupId1, "gsid1");
    }

    public void testGroupInsertFailureOnGroupIdConflict() {
        long rawContactId1 = createRawContact(mAccount);
        long groupId1 = createGroup(mAccount, "gsid1", "title1");

        ContentValues values = new ContentValues();
        values.put(GroupMembership.RAW_CONTACT_ID, rawContactId1);
        values.put(GroupMembership.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE);
        values.put(GroupMembership.GROUP_SOURCE_ID, "gsid1");
        values.put(GroupMembership.GROUP_ROW_ID, groupId1);
        try {
            mResolver.insert(Data.CONTENT_URI, values);
            fail("the insert was expected to fail, but it succeeded");
        } catch (IllegalArgumentException e) {
            // this was expected
        }
    }

    public void testGroupSummaryQuery() {
        final Account account1 = new Account("accountName1", "accountType1");
        final Account account2 = new Account("accountName2", "accountType2");
        final long groupId1 = createGroup(account1, "sourceId1", "title1");
        final long groupId2 = createGroup(account2, "sourceId2", "title2");
        final long groupId3 = createGroup(account2, "sourceId3", "title3");

        // Prepare raw contact id not used at all, to test group summary uri won't be confused
        // with it.
        final long rawContactId0 = createRawContactWithName("firstName0", "lastName0");

        final long rawContactId1 = createRawContactWithName("firstName1", "lastName1");
        insertEmail(rawContactId1, "address1@email.com");
        insertGroupMembership(rawContactId1, groupId1);

        final long rawContactId2 = createRawContactWithName("firstName2", "lastName2");
        insertEmail(rawContactId2, "address2@email.com");
        insertPhoneNumber(rawContactId2, "222-222-2222");
        insertGroupMembership(rawContactId2, groupId1);

        ContentValues v1 = new ContentValues();
        v1.put(Groups._ID, groupId1);
        v1.put(Groups.TITLE, "title1");
        v1.put(Groups.SOURCE_ID, "sourceId1");
        v1.put(Groups.ACCOUNT_NAME, account1.name);
        v1.put(Groups.ACCOUNT_TYPE, account1.type);
        v1.put(Groups.SUMMARY_COUNT, 2);
        v1.put(Groups.SUMMARY_WITH_PHONES, 1);

        ContentValues v2 = new ContentValues();
        v2.put(Groups._ID, groupId2);
        v2.put(Groups.TITLE, "title2");
        v2.put(Groups.SOURCE_ID, "sourceId2");
        v2.put(Groups.ACCOUNT_NAME, account2.name);
        v2.put(Groups.ACCOUNT_TYPE, account2.type);
        v2.put(Groups.SUMMARY_COUNT, 0);
        v2.put(Groups.SUMMARY_WITH_PHONES, 0);

        ContentValues v3 = new ContentValues();
        v3.put(Groups._ID, groupId3);
        v3.put(Groups.TITLE, "title3");
        v3.put(Groups.SOURCE_ID, "sourceId3");
        v3.put(Groups.ACCOUNT_NAME, account2.name);
        v3.put(Groups.ACCOUNT_TYPE, account2.type);
        v3.put(Groups.SUMMARY_COUNT, 0);
        v3.put(Groups.SUMMARY_WITH_PHONES, 0);

        assertStoredValues(Groups.CONTENT_SUMMARY_URI, new ContentValues[] { v1, v2, v3 });

        // Now rawContactId1 has two phone numbers.
        insertPhoneNumber(rawContactId1, "111-111-1111");
        insertPhoneNumber(rawContactId1, "111-111-1112");
        // Result should reflect it correctly (don't count phone numbers but raw contacts)
        v1.put(Groups.SUMMARY_WITH_PHONES, v1.getAsInteger(Groups.SUMMARY_WITH_PHONES) + 1);
        assertStoredValues(Groups.CONTENT_SUMMARY_URI, new ContentValues[] { v1, v2, v3 });

        // Introduce new raw contact, pretending the user added another info.
        final long rawContactId3 = createRawContactWithName("firstName3", "lastName3");
        insertEmail(rawContactId3, "address3@email.com");
        insertPhoneNumber(rawContactId3, "333-333-3333");
        insertGroupMembership(rawContactId3, groupId2);
        v2.put(Groups.SUMMARY_COUNT, v2.getAsInteger(Groups.SUMMARY_COUNT) + 1);
        v2.put(Groups.SUMMARY_WITH_PHONES, v2.getAsInteger(Groups.SUMMARY_WITH_PHONES) + 1);

        assertStoredValues(Groups.CONTENT_SUMMARY_URI, new ContentValues[] { v1, v2, v3 });

        final Uri uri = Groups.CONTENT_SUMMARY_URI.buildUpon()
                .appendQueryParameter(Groups.PARAM_RETURN_GROUP_COUNT_PER_ACCOUNT, "true")
                .build();
        v1.put(Groups.SUMMARY_GROUP_COUNT_PER_ACCOUNT, 1);
        v2.put(Groups.SUMMARY_GROUP_COUNT_PER_ACCOUNT, 2);
        v3.put(Groups.SUMMARY_GROUP_COUNT_PER_ACCOUNT, 2);
        assertStoredValues(uri, new ContentValues[] { v1, v2, v3 });

        // Introduce another group in account1, testing SUMMARY_GROUP_COUNT_PER_ACCOUNT correctly
        // reflects the change.
        final long groupId4 = createGroup(account1, "sourceId4", "title4");
        v1.put(Groups.SUMMARY_GROUP_COUNT_PER_ACCOUNT,
                v1.getAsInteger(Groups.SUMMARY_GROUP_COUNT_PER_ACCOUNT) + 1);
        ContentValues v4 = new ContentValues();
        v4.put(Groups._ID, groupId4);
        v4.put(Groups.TITLE, "title4");
        v4.put(Groups.SOURCE_ID, "sourceId4");
        v4.put(Groups.ACCOUNT_NAME, account1.name);
        v4.put(Groups.ACCOUNT_TYPE, account1.type);
        v4.put(Groups.SUMMARY_COUNT, 0);
        v4.put(Groups.SUMMARY_WITH_PHONES, 0);
        v4.put(Groups.SUMMARY_GROUP_COUNT_PER_ACCOUNT,
                v1.getAsInteger(Groups.SUMMARY_GROUP_COUNT_PER_ACCOUNT));
        assertStoredValues(uri, new ContentValues[] { v1, v2, v3, v4 });

        // We change the tables dynamically according to the requested projection.
        // Make sure the SUMMARY_COUNT column exists
        v1.clear();
        v1.put(Groups.SUMMARY_COUNT, 2);
        v2.clear();
        v2.put(Groups.SUMMARY_COUNT, 1);
        v3.clear();
        v3.put(Groups.SUMMARY_COUNT, 0);
        v4.clear();
        v4.put(Groups.SUMMARY_COUNT, 0);
        assertStoredValuesWithProjection(uri, new ContentValues[] { v1, v2, v3, v4 });
    }

    public void testSettingsQuery() {
        Account account1 = new Account("a", "b");
        Account account2 = new Account("c", "d");
        AccountWithDataSet account3 = new AccountWithDataSet("e", "f", "plus");
        createSettings(account1, "0", "0");
        createSettings(account2, "1", "1");
        createSettings(account3, "1", "0");
        Uri uri1 = maybeAddAccountQueryParameters(Settings.CONTENT_URI, account1);
        Uri uri2 = maybeAddAccountQueryParameters(Settings.CONTENT_URI, account2);
        Uri uri3 = Settings.CONTENT_URI.buildUpon()
                .appendQueryParameter(RawContacts.ACCOUNT_NAME, account3.getAccountName())
                .appendQueryParameter(RawContacts.ACCOUNT_TYPE, account3.getAccountType())
                .appendQueryParameter(RawContacts.DATA_SET, account3.getDataSet())
                .build();
        assertEquals(1, getCount(uri1, null, null));
        assertEquals(1, getCount(uri2, null, null));
        assertEquals(1, getCount(uri3, null, null));
        assertStoredValue(uri1, Settings.SHOULD_SYNC, "0") ;
        assertStoredValue(uri1, Settings.UNGROUPED_VISIBLE, "0");
        assertStoredValue(uri2, Settings.SHOULD_SYNC, "1") ;
        assertStoredValue(uri2, Settings.UNGROUPED_VISIBLE, "1");
        assertStoredValue(uri3, Settings.SHOULD_SYNC, "1");
        assertStoredValue(uri3, Settings.UNGROUPED_VISIBLE, "0");
    }

    public void testSettingsInsertionPreventsDuplicates() {
        Account account1 = new Account("a", "b");
        AccountWithDataSet account2 = new AccountWithDataSet("c", "d", "plus");
        createSettings(account1, "0", "0");
        createSettings(account2, "1", "1");

        // Now try creating the settings rows again.  It should update the existing settings rows.
        createSettings(account1, "1", "0");
        assertStoredValue(Settings.CONTENT_URI,
                Settings.ACCOUNT_NAME + "=? AND " + Settings.ACCOUNT_TYPE + "=?",
                new String[] {"a", "b"}, Settings.SHOULD_SYNC, "1");

        createSettings(account2, "0", "1");
        assertStoredValue(Settings.CONTENT_URI,
                Settings.ACCOUNT_NAME + "=? AND " + Settings.ACCOUNT_TYPE + "=? AND " +
                Settings.DATA_SET + "=?",
                new String[] {"c", "d", "plus"}, Settings.SHOULD_SYNC, "0");
    }

    public void testDisplayNameParsingWhenPartsUnspecified() {
        long rawContactId = createRawContact();
        ContentValues values = new ContentValues();
        values.put(StructuredName.DISPLAY_NAME, "Mr.John Kevin von Smith, Jr.");
        insertStructuredName(rawContactId, values);

        assertStructuredName(rawContactId, "Mr.", "John", "Kevin", "von Smith", "Jr.");
    }

    public void testDisplayNameParsingWhenPartsAreNull() {
        long rawContactId = createRawContact();
        ContentValues values = new ContentValues();
        values.put(StructuredName.DISPLAY_NAME, "Mr.John Kevin von Smith, Jr.");
        values.putNull(StructuredName.GIVEN_NAME);
        values.putNull(StructuredName.FAMILY_NAME);
        insertStructuredName(rawContactId, values);
        assertStructuredName(rawContactId, "Mr.", "John", "Kevin", "von Smith", "Jr.");
    }

    public void testDisplayNameParsingWhenPartsSpecified() {
        long rawContactId = createRawContact();
        ContentValues values = new ContentValues();
        values.put(StructuredName.DISPLAY_NAME, "Mr.John Kevin von Smith, Jr.");
        values.put(StructuredName.FAMILY_NAME, "Johnson");
        insertStructuredName(rawContactId, values);

        assertStructuredName(rawContactId, null, null, null, "Johnson", null);
    }

    public void testContactWithoutPhoneticName() {
        final long rawContactId = createRawContact(null);

        ContentValues values = new ContentValues();
        values.put(StructuredName.PREFIX, "Mr");
        values.put(StructuredName.GIVEN_NAME, "John");
        values.put(StructuredName.MIDDLE_NAME, "K.");
        values.put(StructuredName.FAMILY_NAME, "Doe");
        values.put(StructuredName.SUFFIX, "Jr.");
        Uri dataUri = insertStructuredName(rawContactId, values);

        values.clear();
        values.put(RawContacts.DISPLAY_NAME_SOURCE, DisplayNameSources.STRUCTURED_NAME);
        values.put(RawContacts.DISPLAY_NAME_PRIMARY, "Mr John K. Doe, Jr.");
        values.put(RawContacts.DISPLAY_NAME_ALTERNATIVE, "Mr Doe, John K., Jr.");
        values.putNull(RawContacts.PHONETIC_NAME);
        values.put(RawContacts.PHONETIC_NAME_STYLE, PhoneticNameStyle.UNDEFINED);
        values.put(RawContacts.SORT_KEY_PRIMARY, "John K. Doe, Jr.");
        values.put(RawContacts.SORT_KEY_ALTERNATIVE, "Doe, John K., Jr.");

        Uri rawContactUri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId);
        assertStoredValues(rawContactUri, values);

        values.clear();
        values.put(Contacts.DISPLAY_NAME_SOURCE, DisplayNameSources.STRUCTURED_NAME);
        values.put(Contacts.DISPLAY_NAME_PRIMARY, "Mr John K. Doe, Jr.");
        values.put(Contacts.DISPLAY_NAME_ALTERNATIVE, "Mr Doe, John K., Jr.");
        values.putNull(Contacts.PHONETIC_NAME);
        values.put(Contacts.PHONETIC_NAME_STYLE, PhoneticNameStyle.UNDEFINED);
        values.put(Contacts.SORT_KEY_PRIMARY, "John K. Doe, Jr.");
        values.put(Contacts.SORT_KEY_ALTERNATIVE, "Doe, John K., Jr.");

        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI,
                queryContactId(rawContactId));
        assertStoredValues(contactUri, values);

        // The same values should be available through a join with Data
        assertStoredValues(dataUri, values);
    }

    public void testContactWithChineseName() {

        // Only run this test when Chinese collation is supported
        if (!Arrays.asList(Collator.getAvailableLocales()).contains(Locale.CHINA)) {
            return;
        }

        long rawContactId = createRawContact(null);

        ContentValues values = new ContentValues();
        values.put(StructuredName.DISPLAY_NAME, "\u6BB5\u5C0F\u6D9B");
        Uri dataUri = insertStructuredName(rawContactId, values);

        values.clear();
        values.put(RawContacts.DISPLAY_NAME_SOURCE, DisplayNameSources.STRUCTURED_NAME);
        values.put(RawContacts.DISPLAY_NAME_PRIMARY, "\u6BB5\u5C0F\u6D9B");
        values.put(RawContacts.DISPLAY_NAME_ALTERNATIVE, "\u6BB5\u5C0F\u6D9B");
        values.putNull(RawContacts.PHONETIC_NAME);
        values.put(RawContacts.PHONETIC_NAME_STYLE, PhoneticNameStyle.UNDEFINED);
        values.put(RawContacts.SORT_KEY_PRIMARY, "DUAN \u6BB5 XIAO \u5C0F TAO \u6D9B");
        values.put(RawContacts.SORT_KEY_ALTERNATIVE, "DUAN \u6BB5 XIAO \u5C0F TAO \u6D9B");

        Uri rawContactUri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId);
        assertStoredValues(rawContactUri, values);

        values.clear();
        values.put(Contacts.DISPLAY_NAME_SOURCE, DisplayNameSources.STRUCTURED_NAME);
        values.put(Contacts.DISPLAY_NAME_PRIMARY, "\u6BB5\u5C0F\u6D9B");
        values.put(Contacts.DISPLAY_NAME_ALTERNATIVE, "\u6BB5\u5C0F\u6D9B");
        values.putNull(Contacts.PHONETIC_NAME);
        values.put(Contacts.PHONETIC_NAME_STYLE, PhoneticNameStyle.UNDEFINED);
        values.put(Contacts.SORT_KEY_PRIMARY, "DUAN \u6BB5 XIAO \u5C0F TAO \u6D9B");
        values.put(Contacts.SORT_KEY_ALTERNATIVE, "DUAN \u6BB5 XIAO \u5C0F TAO \u6D9B");

        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI,
                queryContactId(rawContactId));
        assertStoredValues(contactUri, values);

        // The same values should be available through a join with Data
        assertStoredValues(dataUri, values);
    }

    public void testContactWithJapaneseName() {
        long rawContactId = createRawContact(null);

        ContentValues values = new ContentValues();
        values.put(StructuredName.GIVEN_NAME, "\u7A7A\u6D77");
        values.put(StructuredName.PHONETIC_GIVEN_NAME, "\u304B\u3044\u304F\u3046");
        Uri dataUri = insertStructuredName(rawContactId, values);

        values.clear();
        values.put(RawContacts.DISPLAY_NAME_SOURCE, DisplayNameSources.STRUCTURED_NAME);
        values.put(RawContacts.DISPLAY_NAME_PRIMARY, "\u7A7A\u6D77");
        values.put(RawContacts.DISPLAY_NAME_ALTERNATIVE, "\u7A7A\u6D77");
        values.put(RawContacts.PHONETIC_NAME, "\u304B\u3044\u304F\u3046");
        values.put(RawContacts.PHONETIC_NAME_STYLE, PhoneticNameStyle.JAPANESE);
        values.put(RawContacts.SORT_KEY_PRIMARY, "\u304B\u3044\u304F\u3046");
        values.put(RawContacts.SORT_KEY_ALTERNATIVE, "\u304B\u3044\u304F\u3046");

        Uri rawContactUri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId);
        assertStoredValues(rawContactUri, values);

        values.clear();
        values.put(Contacts.DISPLAY_NAME_SOURCE, DisplayNameSources.STRUCTURED_NAME);
        values.put(Contacts.DISPLAY_NAME_PRIMARY, "\u7A7A\u6D77");
        values.put(Contacts.DISPLAY_NAME_ALTERNATIVE, "\u7A7A\u6D77");
        values.put(Contacts.PHONETIC_NAME, "\u304B\u3044\u304F\u3046");
        values.put(Contacts.PHONETIC_NAME_STYLE, PhoneticNameStyle.JAPANESE);
        values.put(Contacts.SORT_KEY_PRIMARY, "\u304B\u3044\u304F\u3046");
        values.put(Contacts.SORT_KEY_ALTERNATIVE, "\u304B\u3044\u304F\u3046");

        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI,
                queryContactId(rawContactId));
        assertStoredValues(contactUri, values);

        // The same values should be available through a join with Data
        assertStoredValues(dataUri, values);
    }

    public void testDisplayNameUpdate() {
        long rawContactId1 = createRawContact();
        insertEmail(rawContactId1, "potato@acme.com", true);

        long rawContactId2 = createRawContact();
        insertPhoneNumber(rawContactId2, "123456789", true);

        setAggregationException(AggregationExceptions.TYPE_KEEP_TOGETHER,
                rawContactId1, rawContactId2);

        assertAggregated(rawContactId1, rawContactId2, "123456789");

        insertStructuredName(rawContactId2, "Potato", "Head");

        assertAggregated(rawContactId1, rawContactId2, "Potato Head");
        assertNetworkNotified(true);
    }

    public void testDisplayNameFromData() {
        long rawContactId = createRawContact();
        long contactId = queryContactId(rawContactId);
        ContentValues values = new ContentValues();

        Uri uri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);

        assertStoredValue(uri, Contacts.DISPLAY_NAME, null);
        insertEmail(rawContactId, "mike@monstersinc.com");
        assertStoredValue(uri, Contacts.DISPLAY_NAME, "mike@monstersinc.com");

        insertEmail(rawContactId, "james@monstersinc.com", true);
        assertStoredValue(uri, Contacts.DISPLAY_NAME, "james@monstersinc.com");

        insertPhoneNumber(rawContactId, "1-800-466-4411");
        assertStoredValue(uri, Contacts.DISPLAY_NAME, "1-800-466-4411");

        // If there are title and company, the company is display name.
        values.clear();
        values.put(Organization.COMPANY, "Monsters Inc");
        Uri organizationUri = insertOrganization(rawContactId, values);
        assertStoredValue(uri, Contacts.DISPLAY_NAME, "Monsters Inc");

        // If there is nickname, that is display name.
        insertNickname(rawContactId, "Sully");
        assertStoredValue(uri, Contacts.DISPLAY_NAME, "Sully");

        // If there is structured name, that is display name.
        values.clear();
        values.put(StructuredName.GIVEN_NAME, "James");
        values.put(StructuredName.MIDDLE_NAME, "P.");
        values.put(StructuredName.FAMILY_NAME, "Sullivan");
        insertStructuredName(rawContactId, values);
        assertStoredValue(uri, Contacts.DISPLAY_NAME, "James P. Sullivan");
    }

    public void testDisplayNameFromOrganizationWithoutPhoneticName() {
        long rawContactId = createRawContact();
        long contactId = queryContactId(rawContactId);
        ContentValues values = new ContentValues();

        Uri uri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);

        // If there is title without company, the title is display name.
        values.clear();
        values.put(Organization.TITLE, "Protagonist");
        Uri organizationUri = insertOrganization(rawContactId, values);
        assertStoredValue(uri, Contacts.DISPLAY_NAME, "Protagonist");

        // If there are title and company, the company is display name.
        values.clear();
        values.put(Organization.COMPANY, "Monsters Inc");
        mResolver.update(organizationUri, values, null, null);

        values.clear();
        values.put(Contacts.DISPLAY_NAME, "Monsters Inc");
        values.putNull(Contacts.PHONETIC_NAME);
        values.put(Contacts.PHONETIC_NAME_STYLE, PhoneticNameStyle.UNDEFINED);
        values.put(Contacts.SORT_KEY_PRIMARY, "Monsters Inc");
        values.put(Contacts.SORT_KEY_ALTERNATIVE, "Monsters Inc");
        assertStoredValues(uri, values);
    }

    public void testDisplayNameFromOrganizationWithJapanesePhoneticName() {
        long rawContactId = createRawContact();
        long contactId = queryContactId(rawContactId);
        ContentValues values = new ContentValues();

        Uri uri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);

        // If there is title without company, the title is display name.
        values.clear();
        values.put(Organization.COMPANY, "DoCoMo");
        values.put(Organization.PHONETIC_NAME, "\u30C9\u30B3\u30E2");
        Uri organizationUri = insertOrganization(rawContactId, values);

        values.clear();
        values.put(Contacts.DISPLAY_NAME, "DoCoMo");
        values.put(Contacts.PHONETIC_NAME, "\u30C9\u30B3\u30E2");
        values.put(Contacts.PHONETIC_NAME_STYLE, PhoneticNameStyle.JAPANESE);
        values.put(Contacts.SORT_KEY_PRIMARY, "\u30C9\u30B3\u30E2");
        values.put(Contacts.SORT_KEY_ALTERNATIVE, "\u30C9\u30B3\u30E2");
        assertStoredValues(uri, values);
    }

    public void testDisplayNameFromOrganizationWithChineseName() {
        boolean hasChineseCollator = false;
        final Locale locale[] = Collator.getAvailableLocales();
        for (int i = 0; i < locale.length; i++) {
            if (locale[i].equals(Locale.CHINA)) {
                hasChineseCollator = true;
                break;
            }
        }

        if (!hasChineseCollator) {
            return;
        }

        long rawContactId = createRawContact();
        long contactId = queryContactId(rawContactId);
        ContentValues values = new ContentValues();

        Uri uri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);

        // If there is title without company, the title is display name.
        values.clear();
        values.put(Organization.COMPANY, "\u4E2D\u56FD\u7535\u4FE1");
        Uri organizationUri = insertOrganization(rawContactId, values);

        values.clear();
        values.put(Contacts.DISPLAY_NAME, "\u4E2D\u56FD\u7535\u4FE1");
        values.putNull(Contacts.PHONETIC_NAME);
        values.put(Contacts.PHONETIC_NAME_STYLE, PhoneticNameStyle.UNDEFINED);
        values.put(Contacts.SORT_KEY_PRIMARY, "ZHONG \u4E2D GUO \u56FD DIAN \u7535 XIN \u4FE1");
        values.put(Contacts.SORT_KEY_ALTERNATIVE, "ZHONG \u4E2D GUO \u56FD DIAN \u7535 XIN \u4FE1");
        assertStoredValues(uri, values);
    }

    public void testLookupByOrganization() {
        long rawContactId = createRawContact();
        long contactId = queryContactId(rawContactId);
        ContentValues values = new ContentValues();

        values.clear();
        values.put(Organization.COMPANY, "acmecorp");
        values.put(Organization.TITLE, "president");
        Uri organizationUri = insertOrganization(rawContactId, values);

        assertContactFilter(contactId, "acmecorp");
        assertContactFilter(contactId, "president");

        values.clear();
        values.put(Organization.DEPARTMENT, "software");
        mResolver.update(organizationUri, values, null, null);

        assertContactFilter(contactId, "acmecorp");
        assertContactFilter(contactId, "president");

        values.clear();
        values.put(Organization.COMPANY, "incredibles");
        mResolver.update(organizationUri, values, null, null);

        assertContactFilter(contactId, "incredibles");
        assertContactFilter(contactId, "president");

        values.clear();
        values.put(Organization.TITLE, "director");
        mResolver.update(organizationUri, values, null, null);

        assertContactFilter(contactId, "incredibles");
        assertContactFilter(contactId, "director");

        values.clear();
        values.put(Organization.COMPANY, "monsters");
        values.put(Organization.TITLE, "scarer");
        mResolver.update(organizationUri, values, null, null);

        assertContactFilter(contactId, "monsters");
        assertContactFilter(contactId, "scarer");
    }

    private void assertContactFilter(long contactId, String filter) {
        Uri filterUri = Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI, Uri.encode(filter));
        assertStoredValue(filterUri, Contacts._ID, contactId);
    }

    private void assertContactFilterNoResult(String filter) {
        Uri filterUri4 = Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI, filter);
        assertEquals(0, getCount(filterUri4, null, null));
    }

    public void testSearchSnippetOrganization() throws Exception {
        long rawContactId = createRawContactWithName();
        long contactId = queryContactId(rawContactId);

        // Some random data element
        insertEmail(rawContactId, "inc@corp.com");

        ContentValues values = new ContentValues();
        values.clear();
        values.put(Organization.COMPANY, "acmecorp");
        values.put(Organization.TITLE, "engineer");
        Uri organizationUri = insertOrganization(rawContactId, values);

        // Add another matching organization
        values.put(Organization.COMPANY, "acmeinc");
        insertOrganization(rawContactId, values);

        // Add another non-matching organization
        values.put(Organization.COMPANY, "corpacme");
        insertOrganization(rawContactId, values);

        // And another data element
        insertEmail(rawContactId, "emca@corp.com", true, Email.TYPE_CUSTOM, "Custom");

        Uri filterUri = buildFilterUri("acme", true);

        values.clear();
        values.put(Contacts._ID, contactId);
        values.put(SearchSnippetColumns.SNIPPET, "engineer, [acmecorp]");
        assertStoredValues(filterUri, values);
    }

    public void testSearchSnippetEmail() throws Exception {
        long rawContactId = createRawContact();
        long contactId = queryContactId(rawContactId);
        ContentValues values = new ContentValues();

        insertStructuredName(rawContactId, "John", "Doe");
        Uri dataUri = insertEmail(rawContactId, "acme@corp.com", true, Email.TYPE_CUSTOM, "Custom");

        Uri filterUri = buildFilterUri("acme", true);

        values.clear();
        values.put(Contacts._ID, contactId);
        values.put(SearchSnippetColumns.SNIPPET, "[acme@corp.com]");
        assertStoredValues(filterUri, values);
    }

    public void testSearchSnippetPhone() throws Exception {
        long rawContactId = createRawContact();
        long contactId = queryContactId(rawContactId);
        ContentValues values = new ContentValues();

        insertStructuredName(rawContactId, "Cave", "Johnson");
        insertPhoneNumber(rawContactId, "(860) 555-1234");

        values.clear();
        values.put(Contacts._ID, contactId);
        values.put(SearchSnippetColumns.SNIPPET, "[(860) 555-1234]");

        assertStoredValues(Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI,
                Uri.encode("86 (0) 5-55-12-34")), values);
        assertStoredValues(Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI,
                Uri.encode("860 555-1234")), values);
        assertStoredValues(Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI,
                Uri.encode("860")), values);
        assertStoredValues(Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI,
                Uri.encode("8605551234")), values);
        assertStoredValues(Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI,
                Uri.encode("860555")), values);
        assertStoredValues(Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI,
                Uri.encode("860 555")), values);
        assertStoredValues(Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI,
                Uri.encode("860-555")), values);
    }

    private Uri buildFilterUri(String query, boolean deferredSnippeting) {
        Uri.Builder builder = Contacts.CONTENT_FILTER_URI.buildUpon()
                .appendPath(Uri.encode(query));
        if (deferredSnippeting) {
            builder.appendQueryParameter(ContactsContract.DEFERRED_SNIPPETING, "1");
        }
        return builder.build();
    }

    public void testSearchSnippetNickname() throws Exception {
        long rawContactId = createRawContactWithName();
        long contactId = queryContactId(rawContactId);
        ContentValues values = new ContentValues();

        Uri dataUri = insertNickname(rawContactId, "Incredible");

        Uri filterUri = buildFilterUri("inc", true);

        values.clear();
        values.put(Contacts._ID, contactId);
        values.put(SearchSnippetColumns.SNIPPET, "[Incredible]");
        assertStoredValues(filterUri, values);
    }

    public void testSearchSnippetEmptyForNameInDisplayName() throws Exception {
        long rawContactId = createRawContact();
        long contactId = queryContactId(rawContactId);
        insertStructuredName(rawContactId, "Cave", "Johnson");
        insertEmail(rawContactId, "cave@aperturescience.com", true);

        ContentValues emptySnippet = new ContentValues();
        emptySnippet.clear();
        emptySnippet.put(Contacts._ID, contactId);
        emptySnippet.put(SearchSnippetColumns.SNIPPET, (String) null);

        assertStoredValues(buildFilterUri("cave", true), emptySnippet);
        assertStoredValues(buildFilterUri("john", true), emptySnippet);
    }

    public void testSearchSnippetEmptyForNicknameInDisplayName() throws Exception {
        long rawContactId = createRawContact();
        long contactId = queryContactId(rawContactId);
        insertNickname(rawContactId, "Caveman");
        insertEmail(rawContactId, "cave@aperturescience.com", true);

        ContentValues emptySnippet = new ContentValues();
        emptySnippet.clear();
        emptySnippet.put(Contacts._ID, contactId);
        emptySnippet.put(SearchSnippetColumns.SNIPPET, (String) null);

        assertStoredValues(buildFilterUri("cave", true), emptySnippet);
    }

    public void testSearchSnippetEmptyForCompanyInDisplayName() throws Exception {
        long rawContactId = createRawContact();
        long contactId = queryContactId(rawContactId);
        ContentValues company = new ContentValues();
        company.clear();
        company.put(Organization.COMPANY, "Aperture Science");
        company.put(Organization.TITLE, "President");
        insertOrganization(rawContactId, company);
        insertEmail(rawContactId, "aperturepresident@aperturescience.com", true);

        ContentValues emptySnippet = new ContentValues();
        emptySnippet.clear();
        emptySnippet.put(Contacts._ID, contactId);
        emptySnippet.put(SearchSnippetColumns.SNIPPET, (String) null);

        assertStoredValues(buildFilterUri("aperture", true), emptySnippet);
    }

    public void testSearchSnippetEmptyForPhoneInDisplayName() throws Exception {
        long rawContactId = createRawContact();
        long contactId = queryContactId(rawContactId);
        insertPhoneNumber(rawContactId, "860-555-1234");
        insertEmail(rawContactId, "860@aperturescience.com", true);

        ContentValues emptySnippet = new ContentValues();
        emptySnippet.clear();
        emptySnippet.put(Contacts._ID, contactId);
        emptySnippet.put(SearchSnippetColumns.SNIPPET, (String) null);

        assertStoredValues(buildFilterUri("860", true), emptySnippet);
    }

    public void testSearchSnippetEmptyForEmailInDisplayName() throws Exception {
        long rawContactId = createRawContact();
        long contactId = queryContactId(rawContactId);
        insertEmail(rawContactId, "cave@aperturescience.com", true);
        insertNote(rawContactId, "Cave Johnson is president of Aperture Science");

        ContentValues emptySnippet = new ContentValues();
        emptySnippet.clear();
        emptySnippet.put(Contacts._ID, contactId);
        emptySnippet.put(SearchSnippetColumns.SNIPPET, (String) null);

        assertStoredValues(buildFilterUri("cave", true), emptySnippet);
    }

    public void testDisplayNameUpdateFromStructuredNameUpdate() {
        long rawContactId = createRawContact();
        Uri nameUri = insertStructuredName(rawContactId, "Slinky", "Dog");

        long contactId = queryContactId(rawContactId);

        Uri uri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
        assertStoredValue(uri, Contacts.DISPLAY_NAME, "Slinky Dog");

        ContentValues values = new ContentValues();
        values.putNull(StructuredName.FAMILY_NAME);

        mResolver.update(nameUri, values, null, null);
        assertStoredValue(uri, Contacts.DISPLAY_NAME, "Slinky");

        values.putNull(StructuredName.GIVEN_NAME);

        mResolver.update(nameUri, values, null, null);
        assertStoredValue(uri, Contacts.DISPLAY_NAME, null);

        values.put(StructuredName.FAMILY_NAME, "Dog");
        mResolver.update(nameUri, values, null, null);

        assertStoredValue(uri, Contacts.DISPLAY_NAME, "Dog");
    }

    public void testInsertDataWithContentProviderOperations() throws Exception {
        ContentProviderOperation cpo1 = ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                .withValues(new ContentValues())
                .build();
        ContentProviderOperation cpo2 = ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                .withValue(StructuredName.GIVEN_NAME, "John")
                .withValue(StructuredName.FAMILY_NAME, "Doe")
                .build();
        ContentProviderResult[] results =
                mResolver.applyBatch(ContactsContract.AUTHORITY, Lists.newArrayList(cpo1, cpo2));
        long contactId = queryContactId(ContentUris.parseId(results[0].uri));
        Uri uri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
        assertStoredValue(uri, Contacts.DISPLAY_NAME, "John Doe");
    }

    public void testSendToVoicemailDefault() {
        long rawContactId = createRawContactWithName();
        long contactId = queryContactId(rawContactId);

        Cursor c = queryContact(contactId);
        assertTrue(c.moveToNext());
        int sendToVoicemail = c.getInt(c.getColumnIndex(Contacts.SEND_TO_VOICEMAIL));
        assertEquals(0, sendToVoicemail);
        c.close();
    }

    public void testSetSendToVoicemailAndRingtone() {
        long rawContactId = createRawContactWithName();
        long contactId = queryContactId(rawContactId);

        updateSendToVoicemailAndRingtone(contactId, true, "foo");
        assertSendToVoicemailAndRingtone(contactId, true, "foo");
        assertNetworkNotified(false);

        updateSendToVoicemailAndRingtoneWithSelection(contactId, false, "bar");
        assertSendToVoicemailAndRingtone(contactId, false, "bar");
        assertNetworkNotified(false);
    }

    public void testSendToVoicemailAndRingtoneAfterAggregation() {
        long rawContactId1 = createRawContactWithName("a", "b");
        long contactId1 = queryContactId(rawContactId1);
        updateSendToVoicemailAndRingtone(contactId1, true, "foo");

        long rawContactId2 = createRawContactWithName("c", "d");
        long contactId2 = queryContactId(rawContactId2);
        updateSendToVoicemailAndRingtone(contactId2, true, "bar");

        // Aggregate them
        setAggregationException(AggregationExceptions.TYPE_KEEP_TOGETHER,
                rawContactId1, rawContactId2);

        // Both contacts had "send to VM", the contact now has the same value
        assertSendToVoicemailAndRingtone(contactId1, true, "foo,bar"); // Either foo or bar
    }

    public void testDoNotSendToVoicemailAfterAggregation() {
        long rawContactId1 = createRawContactWithName("e", "f");
        long contactId1 = queryContactId(rawContactId1);
        updateSendToVoicemailAndRingtone(contactId1, true, null);

        long rawContactId2 = createRawContactWithName("g", "h");
        long contactId2 = queryContactId(rawContactId2);
        updateSendToVoicemailAndRingtone(contactId2, false, null);

        // Aggregate them
        setAggregationException(AggregationExceptions.TYPE_KEEP_TOGETHER,
                rawContactId1, rawContactId2);

        // Since one of the contacts had "don't send to VM" that setting wins for the aggregate
        assertSendToVoicemailAndRingtone(queryContactId(rawContactId1), false, null);
    }

    public void testSetSendToVoicemailAndRingtonePreservedAfterJoinAndSplit() {
        long rawContactId1 = createRawContactWithName("i", "j");
        long contactId1 = queryContactId(rawContactId1);
        updateSendToVoicemailAndRingtone(contactId1, true, "foo");

        long rawContactId2 = createRawContactWithName("k", "l");
        long contactId2 = queryContactId(rawContactId2);
        updateSendToVoicemailAndRingtone(contactId2, false, "bar");

        // Aggregate them
        setAggregationException(AggregationExceptions.TYPE_KEEP_TOGETHER,
                rawContactId1, rawContactId2);

        // Split them
        setAggregationException(AggregationExceptions.TYPE_KEEP_SEPARATE,
                rawContactId1, rawContactId2);

        assertSendToVoicemailAndRingtone(queryContactId(rawContactId1), true, "foo");
        assertSendToVoicemailAndRingtone(queryContactId(rawContactId2), false, "bar");
    }

    public void testStatusUpdateInsert() {
        long rawContactId = createRawContact();
        Uri imUri = insertImHandle(rawContactId, Im.PROTOCOL_AIM, null, "aim");
        long dataId = ContentUris.parseId(imUri);

        ContentValues values = new ContentValues();
        values.put(StatusUpdates.DATA_ID, dataId);
        values.put(StatusUpdates.PROTOCOL, Im.PROTOCOL_AIM);
        values.putNull(StatusUpdates.CUSTOM_PROTOCOL);
        values.put(StatusUpdates.IM_HANDLE, "aim");
        values.put(StatusUpdates.PRESENCE, StatusUpdates.INVISIBLE);
        values.put(StatusUpdates.STATUS, "Hiding");
        values.put(StatusUpdates.STATUS_TIMESTAMP, 100);
        values.put(StatusUpdates.STATUS_RES_PACKAGE, "a.b.c");
        values.put(StatusUpdates.STATUS_ICON, 1234);
        values.put(StatusUpdates.STATUS_LABEL, 2345);

        Uri resultUri = mResolver.insert(StatusUpdates.CONTENT_URI, values);

        assertStoredValues(resultUri, values);

        long contactId = queryContactId(rawContactId);
        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);

        values.clear();
        values.put(Contacts.CONTACT_PRESENCE, StatusUpdates.INVISIBLE);
        values.put(Contacts.CONTACT_STATUS, "Hiding");
        values.put(Contacts.CONTACT_STATUS_TIMESTAMP, 100);
        values.put(Contacts.CONTACT_STATUS_RES_PACKAGE, "a.b.c");
        values.put(Contacts.CONTACT_STATUS_ICON, 1234);
        values.put(Contacts.CONTACT_STATUS_LABEL, 2345);

        assertStoredValues(contactUri, values);

        values.clear();
        values.put(StatusUpdates.DATA_ID, dataId);
        values.put(StatusUpdates.STATUS, "Cloaked");
        values.put(StatusUpdates.STATUS_TIMESTAMP, 200);
        values.put(StatusUpdates.STATUS_RES_PACKAGE, "d.e.f");
        values.put(StatusUpdates.STATUS_ICON, 4321);
        values.put(StatusUpdates.STATUS_LABEL, 5432);
        mResolver.insert(StatusUpdates.CONTENT_URI, values);

        values.clear();
        values.put(Contacts.CONTACT_PRESENCE, StatusUpdates.INVISIBLE);
        values.put(Contacts.CONTACT_STATUS, "Cloaked");
        values.put(Contacts.CONTACT_STATUS_TIMESTAMP, 200);
        values.put(Contacts.CONTACT_STATUS_RES_PACKAGE, "d.e.f");
        values.put(Contacts.CONTACT_STATUS_ICON, 4321);
        values.put(Contacts.CONTACT_STATUS_LABEL, 5432);
        assertStoredValues(contactUri, values);
    }

    public void testStatusUpdateInferAttribution() {
        long rawContactId = createRawContact();
        Uri imUri = insertImHandle(rawContactId, Im.PROTOCOL_AIM, null, "aim");
        long dataId = ContentUris.parseId(imUri);

        ContentValues values = new ContentValues();
        values.put(StatusUpdates.DATA_ID, dataId);
        values.put(StatusUpdates.PROTOCOL, Im.PROTOCOL_AIM);
        values.put(StatusUpdates.IM_HANDLE, "aim");
        values.put(StatusUpdates.STATUS, "Hiding");

        Uri resultUri = mResolver.insert(StatusUpdates.CONTENT_URI, values);

        values.clear();
        values.put(StatusUpdates.DATA_ID, dataId);
        values.put(StatusUpdates.STATUS_LABEL, com.android.internal.R.string.imProtocolAim);
        values.put(StatusUpdates.STATUS, "Hiding");

        assertStoredValues(resultUri, values);
    }

    public void testStatusUpdateMatchingImOrEmail() {
        long rawContactId = createRawContact();
        insertImHandle(rawContactId, Im.PROTOCOL_AIM, null, "aim");
        insertImHandle(rawContactId, Im.PROTOCOL_CUSTOM, "my_im_proto", "my_im");
        insertEmail(rawContactId, "m@acme.com");

        // Match on IM (standard)
        insertStatusUpdate(Im.PROTOCOL_AIM, null, "aim", StatusUpdates.AVAILABLE, "Available",
                StatusUpdates.CAPABILITY_HAS_CAMERA);

        // Match on IM (custom)
        insertStatusUpdate(Im.PROTOCOL_CUSTOM, "my_im_proto", "my_im", StatusUpdates.IDLE, "Idle",
                StatusUpdates.CAPABILITY_HAS_CAMERA | StatusUpdates.CAPABILITY_HAS_VIDEO);

        // Match on Email
        insertStatusUpdate(Im.PROTOCOL_GOOGLE_TALK, null, "m@acme.com", StatusUpdates.AWAY, "Away",
                StatusUpdates.CAPABILITY_HAS_VOICE);

        // No match
        insertStatusUpdate(Im.PROTOCOL_ICQ, null, "12345", StatusUpdates.DO_NOT_DISTURB, "Go away",
                StatusUpdates.CAPABILITY_HAS_CAMERA);

        Cursor c = mResolver.query(StatusUpdates.CONTENT_URI, new String[] {
                StatusUpdates.DATA_ID, StatusUpdates.PROTOCOL, StatusUpdates.CUSTOM_PROTOCOL,
                StatusUpdates.PRESENCE, StatusUpdates.STATUS},
                PresenceColumns.RAW_CONTACT_ID + "=" + rawContactId, null, StatusUpdates.DATA_ID);
        assertTrue(c.moveToNext());
        assertStatusUpdate(c, Im.PROTOCOL_AIM, null, StatusUpdates.AVAILABLE, "Available");
        assertTrue(c.moveToNext());
        assertStatusUpdate(c, Im.PROTOCOL_CUSTOM, "my_im_proto", StatusUpdates.IDLE, "Idle");
        assertTrue(c.moveToNext());
        assertStatusUpdate(c, Im.PROTOCOL_GOOGLE_TALK, null, StatusUpdates.AWAY, "Away");
        assertFalse(c.moveToNext());
        c.close();

        long contactId = queryContactId(rawContactId);
        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);

        ContentValues values = new ContentValues();
        values.put(Contacts.CONTACT_PRESENCE, StatusUpdates.AVAILABLE);
        values.put(Contacts.CONTACT_STATUS, "Available");
        assertStoredValuesWithProjection(contactUri, values);
    }

    public void testStatusUpdateUpdateAndDelete() {
        long rawContactId = createRawContact();
        insertImHandle(rawContactId, Im.PROTOCOL_AIM, null, "aim");

        long contactId = queryContactId(rawContactId);
        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);

        ContentValues values = new ContentValues();
        values.putNull(Contacts.CONTACT_PRESENCE);
        values.putNull(Contacts.CONTACT_STATUS);
        assertStoredValuesWithProjection(contactUri, values);

        insertStatusUpdate(Im.PROTOCOL_AIM, null, "aim", StatusUpdates.AWAY, "BUSY",
                StatusUpdates.CAPABILITY_HAS_CAMERA);
        insertStatusUpdate(Im.PROTOCOL_AIM, null, "aim", StatusUpdates.DO_NOT_DISTURB, "GO AWAY",
                StatusUpdates.CAPABILITY_HAS_CAMERA);
        Uri statusUri =
            insertStatusUpdate(Im.PROTOCOL_AIM, null, "aim", StatusUpdates.AVAILABLE, "Available",
                    StatusUpdates.CAPABILITY_HAS_CAMERA);
        long statusId = ContentUris.parseId(statusUri);

        values.put(Contacts.CONTACT_PRESENCE, StatusUpdates.AVAILABLE);
        values.put(Contacts.CONTACT_STATUS, "Available");
        assertStoredValuesWithProjection(contactUri, values);

        // update status_updates table to set new values for
        //     status_updates.status
        //     status_updates.status_ts
        //     presence
        long updatedTs = 200;
        String testUpdate = "test_update";
        String selection = StatusUpdates.DATA_ID + "=" + statusId;
        values.clear();
        values.put(StatusUpdates.STATUS_TIMESTAMP, updatedTs);
        values.put(StatusUpdates.STATUS, testUpdate);
        values.put(StatusUpdates.PRESENCE, "presence_test");
        mResolver.update(StatusUpdates.CONTENT_URI, values,
                StatusUpdates.DATA_ID + "=" + statusId, null);
        assertStoredValuesWithProjection(StatusUpdates.CONTENT_URI, values);

        // update status_updates table to set new values for columns in status_updates table ONLY
        // i.e., no rows in presence table are to be updated.
        updatedTs = 300;
        testUpdate = "test_update_new";
        selection = StatusUpdates.DATA_ID + "=" + statusId;
        values.clear();
        values.put(StatusUpdates.STATUS_TIMESTAMP, updatedTs);
        values.put(StatusUpdates.STATUS, testUpdate);
        mResolver.update(StatusUpdates.CONTENT_URI, values,
                StatusUpdates.DATA_ID + "=" + statusId, null);
        // make sure the presence column value is still the old value
        values.put(StatusUpdates.PRESENCE, "presence_test");
        assertStoredValuesWithProjection(StatusUpdates.CONTENT_URI, values);

        // update status_updates table to set new values for columns in presence table ONLY
        // i.e., no rows in status_updates table are to be updated.
        selection = StatusUpdates.DATA_ID + "=" + statusId;
        values.clear();
        values.put(StatusUpdates.PRESENCE, "presence_test_new");
        mResolver.update(StatusUpdates.CONTENT_URI, values,
                StatusUpdates.DATA_ID + "=" + statusId, null);
        // make sure the status_updates table is not updated
        values.put(StatusUpdates.STATUS_TIMESTAMP, updatedTs);
        values.put(StatusUpdates.STATUS, testUpdate);
        assertStoredValuesWithProjection(StatusUpdates.CONTENT_URI, values);

        // effect "delete status_updates" operation and expect the following
        //   data deleted from status_updates table
        //   presence set to null
        mResolver.delete(StatusUpdates.CONTENT_URI, StatusUpdates.DATA_ID + "=" + statusId, null);
        values.clear();
        values.putNull(Contacts.CONTACT_PRESENCE);
        assertStoredValuesWithProjection(contactUri, values);
    }

    public void testStatusUpdateUpdateToNull() {
        long rawContactId = createRawContact();
        insertImHandle(rawContactId, Im.PROTOCOL_AIM, null, "aim");

        long contactId = queryContactId(rawContactId);
        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);

        ContentValues values = new ContentValues();
        Uri statusUri =
            insertStatusUpdate(Im.PROTOCOL_AIM, null, "aim", StatusUpdates.AVAILABLE, "Available",
                    StatusUpdates.CAPABILITY_HAS_CAMERA);
        long statusId = ContentUris.parseId(statusUri);

        values.put(Contacts.CONTACT_PRESENCE, StatusUpdates.AVAILABLE);
        values.put(Contacts.CONTACT_STATUS, "Available");
        assertStoredValuesWithProjection(contactUri, values);

        values.clear();
        values.putNull(StatusUpdates.PRESENCE);
        mResolver.update(StatusUpdates.CONTENT_URI, values,
                StatusUpdates.DATA_ID + "=" + statusId, null);

        values.clear();
        values.putNull(Contacts.CONTACT_PRESENCE);
        values.put(Contacts.CONTACT_STATUS, "Available");
        assertStoredValuesWithProjection(contactUri, values);
    }

    public void testStatusUpdateWithTimestamp() {
        long rawContactId = createRawContact();
        insertImHandle(rawContactId, Im.PROTOCOL_AIM, null, "aim");
        insertImHandle(rawContactId, Im.PROTOCOL_GOOGLE_TALK, null, "gtalk");

        long contactId = queryContactId(rawContactId);
        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
        insertStatusUpdate(Im.PROTOCOL_AIM, null, "aim", 0, "Offline", 80,
                StatusUpdates.CAPABILITY_HAS_CAMERA, false);
        insertStatusUpdate(Im.PROTOCOL_AIM, null, "aim", 0, "Available", 100,
                StatusUpdates.CAPABILITY_HAS_CAMERA, false);
        insertStatusUpdate(Im.PROTOCOL_GOOGLE_TALK, null, "gtalk", 0, "Busy", 90,
                StatusUpdates.CAPABILITY_HAS_CAMERA, false);

        // Should return the latest status
        ContentValues values = new ContentValues();
        values.put(Contacts.CONTACT_STATUS_TIMESTAMP, 100);
        values.put(Contacts.CONTACT_STATUS, "Available");
        assertStoredValuesWithProjection(contactUri, values);
    }

    private void assertStatusUpdate(Cursor c, int protocol, String customProtocol, int presence,
            String status) {
        ContentValues values = new ContentValues();
        values.put(StatusUpdates.PROTOCOL, protocol);
        values.put(StatusUpdates.CUSTOM_PROTOCOL, customProtocol);
        values.put(StatusUpdates.PRESENCE, presence);
        values.put(StatusUpdates.STATUS, status);
        assertCursorValues(c, values);
    }

    // Stream item query test cases.

    public void testQueryStreamItemsByRawContactId() {
        long rawContactId = createRawContact(mAccount);
        ContentValues values = buildGenericStreamItemValues();
        insertStreamItem(rawContactId, values, mAccount);
        assertStoredValues(
                Uri.withAppendedPath(
                        ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
                        RawContacts.StreamItems.CONTENT_DIRECTORY),
                values);
    }

    public void testQueryStreamItemsByContactId() {
        long rawContactId = createRawContact();
        long contactId = queryContactId(rawContactId);
        ContentValues values = buildGenericStreamItemValues();
        insertStreamItem(rawContactId, values, null);
        assertStoredValues(
                Uri.withAppendedPath(
                        ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId),
                        Contacts.StreamItems.CONTENT_DIRECTORY),
                values);
    }

    public void testQueryStreamItemsByLookupKey() {
        long rawContactId = createRawContact();
        long contactId = queryContactId(rawContactId);
        String lookupKey = queryLookupKey(contactId);
        ContentValues values = buildGenericStreamItemValues();
        insertStreamItem(rawContactId, values, null);
        assertStoredValues(
                Uri.withAppendedPath(
                        Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, lookupKey),
                        Contacts.StreamItems.CONTENT_DIRECTORY),
                values);
    }

    public void testQueryStreamItemsByLookupKeyAndContactId() {
        long rawContactId = createRawContact();
        long contactId = queryContactId(rawContactId);
        String lookupKey = queryLookupKey(contactId);
        ContentValues values = buildGenericStreamItemValues();
        insertStreamItem(rawContactId, values, null);
        assertStoredValues(
                Uri.withAppendedPath(
                        ContentUris.withAppendedId(
                                Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, lookupKey),
                                contactId),
                        Contacts.StreamItems.CONTENT_DIRECTORY),
                values);
    }

    public void testQueryStreamItems() {
        long rawContactId = createRawContact();
        ContentValues values = buildGenericStreamItemValues();
        insertStreamItem(rawContactId, values, null);
        assertStoredValues(StreamItems.CONTENT_URI, values);
    }

    public void testQueryStreamItemsWithSelection() {
        long rawContactId = createRawContact();
        ContentValues firstValues = buildGenericStreamItemValues();
        insertStreamItem(rawContactId, firstValues, null);

        ContentValues secondValues = buildGenericStreamItemValues();
        secondValues.put(StreamItems.TEXT, "Goodbye world");
        insertStreamItem(rawContactId, secondValues, null);

        // Select only the first stream item.
        assertStoredValues(StreamItems.CONTENT_URI, StreamItems.TEXT + "=?",
                new String[]{"Hello world"}, firstValues);

        // Select only the second stream item.
        assertStoredValues(StreamItems.CONTENT_URI, StreamItems.TEXT + "=?",
                new String[]{"Goodbye world"}, secondValues);
    }

    public void testQueryStreamItemById() {
        long rawContactId = createRawContact();
        ContentValues firstValues = buildGenericStreamItemValues();
        Uri resultUri = insertStreamItem(rawContactId, firstValues, null);
        long firstStreamItemId = ContentUris.parseId(resultUri);

        ContentValues secondValues = buildGenericStreamItemValues();
        secondValues.put(StreamItems.TEXT, "Goodbye world");
        resultUri = insertStreamItem(rawContactId, secondValues, null);
        long secondStreamItemId = ContentUris.parseId(resultUri);

        // Select only the first stream item.
        assertStoredValues(ContentUris.withAppendedId(StreamItems.CONTENT_URI, firstStreamItemId),
                firstValues);

        // Select only the second stream item.
        assertStoredValues(ContentUris.withAppendedId(StreamItems.CONTENT_URI, secondStreamItemId),
                secondValues);
    }

    // Stream item photo insertion + query test cases.

    public void testQueryStreamItemPhotoWithSelection() {
        long rawContactId = createRawContact();
        ContentValues values = buildGenericStreamItemValues();
        Uri resultUri = insertStreamItem(rawContactId, values, null);
        long streamItemId = ContentUris.parseId(resultUri);

        ContentValues photo1Values = buildGenericStreamItemPhotoValues(1);
        insertStreamItemPhoto(streamItemId, photo1Values, null);
        photo1Values.remove(StreamItemPhotos.PHOTO);  // Removed during processing.
        ContentValues photo2Values = buildGenericStreamItemPhotoValues(2);
        insertStreamItemPhoto(streamItemId, photo2Values, null);

        // Select only the first photo.
        assertStoredValues(StreamItems.CONTENT_PHOTO_URI, StreamItemPhotos.SORT_INDEX + "=?",
                new String[]{"1"}, photo1Values);
    }

    public void testQueryStreamItemPhotoByStreamItemId() {
        long rawContactId = createRawContact();

        // Insert a first stream item.
        ContentValues firstValues = buildGenericStreamItemValues();
        Uri resultUri = insertStreamItem(rawContactId, firstValues, null);
        long firstStreamItemId = ContentUris.parseId(resultUri);

        // Insert a second stream item.
        ContentValues secondValues = buildGenericStreamItemValues();
        resultUri = insertStreamItem(rawContactId, secondValues, null);
        long secondStreamItemId = ContentUris.parseId(resultUri);

        // Add a photo to the first stream item.
        ContentValues photo1Values = buildGenericStreamItemPhotoValues(1);
        insertStreamItemPhoto(firstStreamItemId, photo1Values, null);
        photo1Values.remove(StreamItemPhotos.PHOTO);  // Removed during processing.

        // Add a photo to the second stream item.
        ContentValues photo2Values = buildGenericStreamItemPhotoValues(1);
        photo2Values.put(StreamItemPhotos.PHOTO, loadPhotoFromResource(
                R.drawable.nebula, PhotoSize.ORIGINAL));
        insertStreamItemPhoto(secondStreamItemId, photo2Values, null);
        photo2Values.remove(StreamItemPhotos.PHOTO);  // Removed during processing.

        // Select only the photos from the second stream item.
        assertStoredValues(Uri.withAppendedPath(
                ContentUris.withAppendedId(StreamItems.CONTENT_URI, secondStreamItemId),
                StreamItems.StreamItemPhotos.CONTENT_DIRECTORY), photo2Values);
    }

    public void testQueryStreamItemPhotoByStreamItemPhotoId() {
        long rawContactId = createRawContact();

        // Insert a first stream item.
        ContentValues firstValues = buildGenericStreamItemValues();
        Uri resultUri = insertStreamItem(rawContactId, firstValues, null);
        long firstStreamItemId = ContentUris.parseId(resultUri);

        // Insert a second stream item.
        ContentValues secondValues = buildGenericStreamItemValues();
        resultUri = insertStreamItem(rawContactId, secondValues, null);
        long secondStreamItemId = ContentUris.parseId(resultUri);

        // Add a photo to the first stream item.
        ContentValues photo1Values = buildGenericStreamItemPhotoValues(1);
        resultUri = insertStreamItemPhoto(firstStreamItemId, photo1Values, null);
        long firstPhotoId = ContentUris.parseId(resultUri);
        photo1Values.remove(StreamItemPhotos.PHOTO);  // Removed during processing.

        // Add a photo to the second stream item.
        ContentValues photo2Values = buildGenericStreamItemPhotoValues(1);
        photo2Values.put(StreamItemPhotos.PHOTO, loadPhotoFromResource(
                R.drawable.galaxy, PhotoSize.ORIGINAL));
        resultUri = insertStreamItemPhoto(secondStreamItemId, photo2Values, null);
        long secondPhotoId = ContentUris.parseId(resultUri);
        photo2Values.remove(StreamItemPhotos.PHOTO);  // Removed during processing.

        // Select the first photo.
        assertStoredValues(ContentUris.withAppendedId(
                Uri.withAppendedPath(
                        ContentUris.withAppendedId(StreamItems.CONTENT_URI, firstStreamItemId),
                        StreamItems.StreamItemPhotos.CONTENT_DIRECTORY),
                firstPhotoId),
                photo1Values);

        // Select the second photo.
        assertStoredValues(ContentUris.withAppendedId(
                Uri.withAppendedPath(
                        ContentUris.withAppendedId(StreamItems.CONTENT_URI, secondStreamItemId),
                        StreamItems.StreamItemPhotos.CONTENT_DIRECTORY),
                secondPhotoId),
                photo2Values);
    }

    // Stream item insertion test cases.

    public void testInsertStreamItemIntoOtherAccount() {
        long rawContactId = createRawContact(mAccount);
        ContentValues values = buildGenericStreamItemValues();
        try {
            insertStreamItem(rawContactId, values, mAccountTwo);
            fail("Stream insertion was allowed in another account's raw contact.");
        } catch (SecurityException expected) {
            // Trying to insert stream items into account one's raw contact is forbidden.
        }
    }

    public void testInsertStreamItemInProfileRequiresWriteProfileAccess() {
        long profileRawContactId = createBasicProfileContact(new ContentValues());

        // With our (default) write profile permission, we should be able to insert a stream item.
        ContentValues values = buildGenericStreamItemValues();
        insertStreamItem(profileRawContactId, values, null);

        // Now take away write profile permission.
        mActor.removePermissions("android.permission.WRITE_PROFILE");

        // Try inserting another stream item.
        try {
            insertStreamItem(profileRawContactId, values, null);
            fail("Should require WRITE_PROFILE access to insert a stream item in the profile.");
        } catch (SecurityException expected) {
            // Trying to insert a stream item in the profile without WRITE_PROFILE permission
            // should fail.
        }
    }

    public void testInsertStreamItemWithContentValues() {
        long rawContactId = createRawContact();
        ContentValues values = buildGenericStreamItemValues();
        values.put(StreamItems.RAW_CONTACT_ID, rawContactId);
        mResolver.insert(StreamItems.CONTENT_URI, values);
        assertStoredValues(Uri.withAppendedPath(
                ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
                RawContacts.StreamItems.CONTENT_DIRECTORY), values);
    }

    public void testInsertStreamItemOverLimit() {
        long rawContactId = createRawContact();
        ContentValues values = buildGenericStreamItemValues();
        values.put(StreamItems.RAW_CONTACT_ID, rawContactId);

        List<Long> streamItemIds = Lists.newArrayList();

        // Insert MAX + 1 stream items.
        long baseTime = System.currentTimeMillis();
        for (int i = 0; i < 6; i++) {
            values.put(StreamItems.TIMESTAMP, baseTime + i);
            Uri resultUri = mResolver.insert(StreamItems.CONTENT_URI, values);
            streamItemIds.add(ContentUris.parseId(resultUri));
        }
        Long doomedStreamItemId = streamItemIds.get(0);

        // There should only be MAX items.  The oldest one should have been cleaned up.
        Cursor c = mResolver.query(
                Uri.withAppendedPath(
                        ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
                        RawContacts.StreamItems.CONTENT_DIRECTORY),
                new String[]{StreamItems._ID}, null, null, null);
        try {
            while(c.moveToNext()) {
                long streamItemId = c.getLong(0);
                streamItemIds.remove(streamItemId);
            }
        } finally {
            c.close();
        }

        assertEquals(1, streamItemIds.size());
        assertEquals(doomedStreamItemId, streamItemIds.get(0));
    }

    public void testInsertStreamItemOlderThanOldestInLimit() {
        long rawContactId = createRawContact();
        ContentValues values = buildGenericStreamItemValues();
        values.put(StreamItems.RAW_CONTACT_ID, rawContactId);

        // Insert MAX stream items.
        long baseTime = System.currentTimeMillis();
        for (int i = 0; i < 5; i++) {
            values.put(StreamItems.TIMESTAMP, baseTime + i);
            Uri resultUri = mResolver.insert(StreamItems.CONTENT_URI, values);
            assertNotSame("Expected non-0 stream item ID to be inserted",
                    0L, ContentUris.parseId(resultUri));
        }

        // Now try to insert a stream item that's older.  It should be deleted immediately
        // and return an ID of 0.
        values.put(StreamItems.TIMESTAMP, baseTime - 1);
        Uri resultUri = mResolver.insert(StreamItems.CONTENT_URI, values);
        assertEquals(0L, ContentUris.parseId(resultUri));
    }

    // Stream item photo insertion test cases.

    public void testInsertStreamItemsAndPhotosInBatch() throws Exception {
        long rawContactId = createRawContact();
        ContentValues streamItemValues = buildGenericStreamItemValues();
        ContentValues streamItemPhotoValues = buildGenericStreamItemPhotoValues(0);

        ArrayList<ContentProviderOperation> ops = Lists.newArrayList();
        ops.add(ContentProviderOperation.newInsert(
                Uri.withAppendedPath(
                        ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
                        RawContacts.StreamItems.CONTENT_DIRECTORY))
                .withValues(streamItemValues).build());
        for (int i = 0; i < 5; i++) {
            streamItemPhotoValues.put(StreamItemPhotos.SORT_INDEX, i);
            ops.add(ContentProviderOperation.newInsert(StreamItems.CONTENT_PHOTO_URI)
                    .withValues(streamItemPhotoValues)
                    .withValueBackReference(StreamItemPhotos.STREAM_ITEM_ID, 0)
                    .build());
        }
        mResolver.applyBatch(ContactsContract.AUTHORITY, ops);

        // Check that all five photos were inserted under the raw contact.
        Cursor c = mResolver.query(StreamItems.CONTENT_URI, new String[]{StreamItems._ID},
                StreamItems.RAW_CONTACT_ID + "=?", new String[]{String.valueOf(rawContactId)},
                null);
        long streamItemId = 0;
        try {
            assertEquals(1, c.getCount());
            c.moveToFirst();
            streamItemId = c.getLong(0);
        } finally {
            c.close();
        }

        c = mResolver.query(Uri.withAppendedPath(
                ContentUris.withAppendedId(StreamItems.CONTENT_URI, streamItemId),
                StreamItems.StreamItemPhotos.CONTENT_DIRECTORY),
                new String[]{StreamItemPhotos._ID, StreamItemPhotos.PHOTO_URI},
                null, null, null);
        try {
            assertEquals(5, c.getCount());
            byte[] expectedPhotoBytes = loadPhotoFromResource(
                    R.drawable.earth_normal, PhotoSize.DISPLAY_PHOTO);
            while (c.moveToNext()) {
                String photoUri = c.getString(1);
                assertInputStreamContent(expectedPhotoBytes,
                        mResolver.openInputStream(Uri.parse(photoUri)));
            }
        } finally {
            c.close();
        }
    }

    // Stream item update test cases.

    public void testUpdateStreamItemById() {
        long rawContactId = createRawContact();
        ContentValues values = buildGenericStreamItemValues();
        Uri resultUri = insertStreamItem(rawContactId, values, null);
        long streamItemId = ContentUris.parseId(resultUri);
        values.put(StreamItems.TEXT, "Goodbye world");
        mResolver.update(ContentUris.withAppendedId(StreamItems.CONTENT_URI, streamItemId), values,
                null, null);
        assertStoredValues(Uri.withAppendedPath(
                ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
                RawContacts.StreamItems.CONTENT_DIRECTORY), values);
    }

    public void testUpdateStreamItemWithContentValues() {
        long rawContactId = createRawContact();
        ContentValues values = buildGenericStreamItemValues();
        Uri resultUri = insertStreamItem(rawContactId, values, null);
        long streamItemId = ContentUris.parseId(resultUri);
        values.put(StreamItems._ID, streamItemId);
        values.put(StreamItems.TEXT, "Goodbye world");
        mResolver.update(StreamItems.CONTENT_URI, values, null, null);
        assertStoredValues(Uri.withAppendedPath(
                ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
                RawContacts.StreamItems.CONTENT_DIRECTORY), values);
    }

    public void testUpdateStreamItemFromOtherAccount() {
        long rawContactId = createRawContact(mAccount);
        ContentValues values = buildGenericStreamItemValues();
        Uri resultUri = insertStreamItem(rawContactId, values, mAccount);
        long streamItemId = ContentUris.parseId(resultUri);
        values.put(StreamItems._ID, streamItemId);
        values.put(StreamItems.TEXT, "Goodbye world");
        try {
            mResolver.update(maybeAddAccountQueryParameters(StreamItems.CONTENT_URI, mAccountTwo),
                    values, null, null);
            fail("Should not be able to update stream items inserted by another account");
        } catch (SecurityException expected) {
            // Can't update the stream items from another account.
        }
    }

    // Stream item photo update test cases.

    public void testUpdateStreamItemPhotoById() throws IOException {
        long rawContactId = createRawContact();
        ContentValues values = buildGenericStreamItemValues();
        Uri resultUri = insertStreamItem(rawContactId, values, null);
        long streamItemId = ContentUris.parseId(resultUri);
        ContentValues photoValues = buildGenericStreamItemPhotoValues(1);
        resultUri = insertStreamItemPhoto(streamItemId, photoValues, null);
        long streamItemPhotoId = ContentUris.parseId(resultUri);

        photoValues.put(StreamItemPhotos.PHOTO, loadPhotoFromResource(
                R.drawable.nebula, PhotoSize.ORIGINAL));
        Uri photoUri =
                ContentUris.withAppendedId(
                        Uri.withAppendedPath(
                                ContentUris.withAppendedId(StreamItems.CONTENT_URI, streamItemId),
                                StreamItems.StreamItemPhotos.CONTENT_DIRECTORY),
                        streamItemPhotoId);
        mResolver.update(photoUri, photoValues, null, null);
        photoValues.remove(StreamItemPhotos.PHOTO);  // Removed during processing.
        assertStoredValues(photoUri, photoValues);

        // Check that the photo stored is the expected one.
        String displayPhotoUri = getStoredValue(photoUri, StreamItemPhotos.PHOTO_URI);
        assertInputStreamContent(loadPhotoFromResource(R.drawable.nebula, PhotoSize.DISPLAY_PHOTO),
                mResolver.openInputStream(Uri.parse(displayPhotoUri)));
    }

    public void testUpdateStreamItemPhotoWithContentValues() throws IOException {
        long rawContactId = createRawContact();
        ContentValues values = buildGenericStreamItemValues();
        Uri resultUri = insertStreamItem(rawContactId, values, null);
        long streamItemId = ContentUris.parseId(resultUri);
        ContentValues photoValues = buildGenericStreamItemPhotoValues(1);
        resultUri = insertStreamItemPhoto(streamItemId, photoValues, null);
        long streamItemPhotoId = ContentUris.parseId(resultUri);

        photoValues.put(StreamItemPhotos._ID, streamItemPhotoId);
        photoValues.put(StreamItemPhotos.PHOTO, loadPhotoFromResource(
                R.drawable.nebula, PhotoSize.ORIGINAL));
        Uri photoUri =
                Uri.withAppendedPath(
                        ContentUris.withAppendedId(StreamItems.CONTENT_URI, streamItemId),
                        StreamItems.StreamItemPhotos.CONTENT_DIRECTORY);
        mResolver.update(photoUri, photoValues, null, null);
        photoValues.remove(StreamItemPhotos.PHOTO);  // Removed during processing.
        assertStoredValues(photoUri, photoValues);

        // Check that the photo stored is the expected one.
        String displayPhotoUri = getStoredValue(photoUri, StreamItemPhotos.PHOTO_URI);
        assertInputStreamContent(loadPhotoFromResource(R.drawable.nebula, PhotoSize.DISPLAY_PHOTO),
                mResolver.openInputStream(Uri.parse(displayPhotoUri)));
    }

    public void testUpdateStreamItemPhotoFromOtherAccount() {
        long rawContactId = createRawContact(mAccount);
        ContentValues values = buildGenericStreamItemValues();
        Uri resultUri = insertStreamItem(rawContactId, values, mAccount);
        long streamItemId = ContentUris.parseId(resultUri);
        ContentValues photoValues = buildGenericStreamItemPhotoValues(1);
        resultUri = insertStreamItemPhoto(streamItemId, photoValues, mAccount);
        long streamItemPhotoId = ContentUris.parseId(resultUri);

        photoValues.put(StreamItemPhotos._ID, streamItemPhotoId);
        photoValues.put(StreamItemPhotos.PHOTO, loadPhotoFromResource(
                R.drawable.galaxy, PhotoSize.ORIGINAL));
        Uri photoUri =
                maybeAddAccountQueryParameters(
                        Uri.withAppendedPath(
                                ContentUris.withAppendedId(StreamItems.CONTENT_URI, streamItemId),
                                StreamItems.StreamItemPhotos.CONTENT_DIRECTORY),
                        mAccountTwo);
        try {
            mResolver.update(photoUri, photoValues, null, null);
            fail("Should not be able to update stream item photos inserted by another account");
        } catch (SecurityException expected) {
            // Can't update a stream item photo inserted by another account.
        }
    }

    // Stream item deletion test cases.

    public void testDeleteStreamItemById() {
        long rawContactId = createRawContact();
        ContentValues firstValues = buildGenericStreamItemValues();
        Uri resultUri = insertStreamItem(rawContactId, firstValues, null);
        long firstStreamItemId = ContentUris.parseId(resultUri);

        ContentValues secondValues = buildGenericStreamItemValues();
        secondValues.put(StreamItems.TEXT, "Goodbye world");
        insertStreamItem(rawContactId, secondValues, null);

        // Delete the first stream item.
        mResolver.delete(ContentUris.withAppendedId(StreamItems.CONTENT_URI, firstStreamItemId),
                null, null);

        // Check that only the second item remains.
        assertStoredValues(Uri.withAppendedPath(
                ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
                RawContacts.StreamItems.CONTENT_DIRECTORY), secondValues);
    }

    public void testDeleteStreamItemWithSelection() {
        long rawContactId = createRawContact();
        ContentValues firstValues = buildGenericStreamItemValues();
        insertStreamItem(rawContactId, firstValues, null);

        ContentValues secondValues = buildGenericStreamItemValues();
        secondValues.put(StreamItems.TEXT, "Goodbye world");
        insertStreamItem(rawContactId, secondValues, null);

        // Delete the first stream item with a custom selection.
        mResolver.delete(StreamItems.CONTENT_URI, StreamItems.TEXT + "=?",
                new String[]{"Hello world"});

        // Check that only the second item remains.
        assertStoredValues(Uri.withAppendedPath(
                ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
                RawContacts.StreamItems.CONTENT_DIRECTORY), secondValues);
    }

    public void testDeleteStreamItemFromOtherAccount() {
        long rawContactId = createRawContact(mAccount);
        long streamItemId = ContentUris.parseId(
                insertStreamItem(rawContactId, buildGenericStreamItemValues(), mAccount));
        try {
            mResolver.delete(
                    maybeAddAccountQueryParameters(
                            ContentUris.withAppendedId(StreamItems.CONTENT_URI, streamItemId),
                            mAccountTwo), null, null);
            fail("Should not be able to delete stream item inserted by another account");
        } catch (SecurityException expected) {
            // Can't delete a stream item from another account.
        }
    }

    // Stream item photo deletion test cases.

    public void testDeleteStreamItemPhotoById() {
        long rawContactId = createRawContact();
        long streamItemId = ContentUris.parseId(
                insertStreamItem(rawContactId, buildGenericStreamItemValues(), null));
        long streamItemPhotoId = ContentUris.parseId(
                insertStreamItemPhoto(streamItemId, buildGenericStreamItemPhotoValues(0), null));
        mResolver.delete(
                ContentUris.withAppendedId(
                        Uri.withAppendedPath(
                                ContentUris.withAppendedId(StreamItems.CONTENT_URI, streamItemId),
                                StreamItems.StreamItemPhotos.CONTENT_DIRECTORY),
                        streamItemPhotoId), null, null);

        Cursor c = mResolver.query(StreamItems.CONTENT_PHOTO_URI,
                new String[]{StreamItemPhotos._ID},
                StreamItemPhotos.STREAM_ITEM_ID + "=?", new String[]{String.valueOf(streamItemId)},
                null);
        try {
            assertEquals("Expected photo to be deleted.", 0, c.getCount());
        } finally {
            c.close();
        }
    }

    public void testDeleteStreamItemPhotoWithSelection() {
        long rawContactId = createRawContact();
        long streamItemId = ContentUris.parseId(
                insertStreamItem(rawContactId, buildGenericStreamItemValues(), null));
        ContentValues firstPhotoValues = buildGenericStreamItemPhotoValues(0);
        ContentValues secondPhotoValues = buildGenericStreamItemPhotoValues(1);
        insertStreamItemPhoto(streamItemId, firstPhotoValues, null);
        firstPhotoValues.remove(StreamItemPhotos.PHOTO);  // Removed while processing.
        insertStreamItemPhoto(streamItemId, secondPhotoValues, null);
        Uri photoUri = Uri.withAppendedPath(
                ContentUris.withAppendedId(StreamItems.CONTENT_URI, streamItemId),
                StreamItems.StreamItemPhotos.CONTENT_DIRECTORY);
        mResolver.delete(photoUri, StreamItemPhotos.SORT_INDEX + "=1", null);

        assertStoredValues(photoUri, firstPhotoValues);
    }

    public void testDeleteStreamItemPhotoFromOtherAccount() {
        long rawContactId = createRawContact(mAccount);
        long streamItemId = ContentUris.parseId(
                insertStreamItem(rawContactId, buildGenericStreamItemValues(), mAccount));
        insertStreamItemPhoto(streamItemId, buildGenericStreamItemPhotoValues(0), mAccount);
        try {
            mResolver.delete(maybeAddAccountQueryParameters(
                    Uri.withAppendedPath(
                            ContentUris.withAppendedId(StreamItems.CONTENT_URI, streamItemId),
                            StreamItems.StreamItemPhotos.CONTENT_DIRECTORY),
                    mAccountTwo), null, null);
            fail("Should not be able to delete stream item photo inserted by another account");
        } catch (SecurityException expected) {
            // Can't delete a stream item photo from another account.
        }
    }

    public void testDeleteStreamItemsWhenRawContactDeleted() {
        long rawContactId = createRawContact(mAccount);
        Uri streamItemUri = insertStreamItem(rawContactId,
                buildGenericStreamItemValues(), mAccount);
        Uri streamItemPhotoUri = insertStreamItemPhoto(ContentUris.parseId(streamItemUri),
                        buildGenericStreamItemPhotoValues(0), mAccount);
        mResolver.delete(ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
                null, null);

        ContentValues[] emptyValues = new ContentValues[0];

        // The stream item and its photo should be gone.
        assertStoredValues(streamItemUri, emptyValues);
        assertStoredValues(streamItemPhotoUri, emptyValues);
    }

    public void testQueryStreamItemLimit() {
        ContentValues values = new ContentValues();
        values.put(StreamItems.MAX_ITEMS, 5);
        assertStoredValues(StreamItems.CONTENT_LIMIT_URI, values);
    }

    // Tests for inserting or updating stream items as a side-effect of making status updates
    // (forward-compatibility of status updates into the new social stream API).

    public void testStreamItemInsertedOnStatusUpdate() {

        // This method of creating a raw contact automatically inserts a status update with
        // the status message "hacking".
        ContentValues values = new ContentValues();
        long rawContactId = createRawContact(values, "18004664411",
                "goog411@acme.com", StatusUpdates.INVISIBLE, 4, 1, 0,
                StatusUpdates.CAPABILITY_HAS_CAMERA | StatusUpdates.CAPABILITY_HAS_VIDEO |
                        StatusUpdates.CAPABILITY_HAS_VOICE);

        ContentValues expectedValues = new ContentValues();
        expectedValues.put(StreamItems.RAW_CONTACT_ID, rawContactId);
        expectedValues.put(StreamItems.TEXT, "hacking");
        assertStoredValues(RawContacts.CONTENT_URI.buildUpon()
                .appendPath(String.valueOf(rawContactId))
                .appendPath(RawContacts.StreamItems.CONTENT_DIRECTORY).build(),
                expectedValues);
    }

    public void testStreamItemInsertedOnStatusUpdate_HtmlQuoting() {

        // This method of creating a raw contact automatically inserts a status update with
        // the status message "hacking".
        ContentValues values = new ContentValues();
        long rawContactId = createRawContact(values, "18004664411",
                "goog411@acme.com", StatusUpdates.INVISIBLE, 4, 1, 0,
                StatusUpdates.CAPABILITY_HAS_VOICE);

        // Insert a new status update for the raw contact.
        insertStatusUpdate(Im.PROTOCOL_GOOGLE_TALK, null, "goog411@acme.com",
                StatusUpdates.INVISIBLE, "& <b> test &#39;", StatusUpdates.CAPABILITY_HAS_VOICE);

        ContentValues expectedValues = new ContentValues();
        expectedValues.put(StreamItems.RAW_CONTACT_ID, rawContactId);
        expectedValues.put(StreamItems.TEXT, "&amp; &lt;b&gt; test &amp;#39;");
        assertStoredValues(RawContacts.CONTENT_URI.buildUpon()
                .appendPath(String.valueOf(rawContactId))
                .appendPath(RawContacts.StreamItems.CONTENT_DIRECTORY).build(),
                expectedValues);
    }

    public void testStreamItemUpdatedOnSecondStatusUpdate() {

        // This method of creating a raw contact automatically inserts a status update with
        // the status message "hacking".
        ContentValues values = new ContentValues();
        int chatMode = StatusUpdates.CAPABILITY_HAS_CAMERA | StatusUpdates.CAPABILITY_HAS_VIDEO |
                StatusUpdates.CAPABILITY_HAS_VOICE;
        long rawContactId = createRawContact(values, "18004664411",
                "goog411@acme.com", StatusUpdates.INVISIBLE, 4, 1, 0, chatMode);

        // Insert a new status update for the raw contact.
        insertStatusUpdate(Im.PROTOCOL_GOOGLE_TALK, null, "goog411@acme.com",
                StatusUpdates.INVISIBLE, "finished hacking", chatMode);

        ContentValues expectedValues = new ContentValues();
        expectedValues.put(StreamItems.RAW_CONTACT_ID, rawContactId);
        expectedValues.put(StreamItems.TEXT, "finished hacking");
        assertStoredValues(RawContacts.CONTENT_URI.buildUpon()
                .appendPath(String.valueOf(rawContactId))
                .appendPath(RawContacts.StreamItems.CONTENT_DIRECTORY).build(),
                expectedValues);
    }

    public void testStreamItemReadRequiresReadSocialStreamPermission() {
        long rawContactId = createRawContact();
        long contactId = queryContactId(rawContactId);
        String lookupKey = queryLookupKey(contactId);
        long streamItemId = ContentUris.parseId(
                insertStreamItem(rawContactId, buildGenericStreamItemValues(), null));
        mActor.removePermissions("android.permission.READ_SOCIAL_STREAM");

        // Try selecting the stream item in various ways.
        expectSecurityException(
                "Querying stream items by contact ID requires social stream read permission",
                Uri.withAppendedPath(
                        ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId),
                        Contacts.StreamItems.CONTENT_DIRECTORY), null, null, null, null);

        expectSecurityException(
                "Querying stream items by lookup key requires social stream read permission",
                Contacts.CONTENT_LOOKUP_URI.buildUpon().appendPath(lookupKey)
                        .appendPath(Contacts.StreamItems.CONTENT_DIRECTORY).build(),
                null, null, null, null);

        expectSecurityException(
                "Querying stream items by lookup key and ID requires social stream read permission",
                Uri.withAppendedPath(Contacts.getLookupUri(contactId, lookupKey),
                        Contacts.StreamItems.CONTENT_DIRECTORY),
                null, null, null, null);

        expectSecurityException(
                "Querying stream items by raw contact ID requires social stream read permission",
                Uri.withAppendedPath(
                        ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
                        RawContacts.StreamItems.CONTENT_DIRECTORY), null, null, null, null);

        expectSecurityException(
                "Querying stream items by raw contact ID and stream item ID requires social " +
                        "stream read permission",
                ContentUris.withAppendedId(
                        Uri.withAppendedPath(
                                ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
                                RawContacts.StreamItems.CONTENT_DIRECTORY),
                        streamItemId), null, null, null, null);

        expectSecurityException(
                "Querying all stream items requires social stream read permission",
                StreamItems.CONTENT_URI, null, null, null, null);

        expectSecurityException(
                "Querying stream item by ID requires social stream read permission",
                ContentUris.withAppendedId(StreamItems.CONTENT_URI, streamItemId),
                null, null, null, null);
    }

    public void testStreamItemPhotoReadRequiresReadSocialStreamPermission() {
        long rawContactId = createRawContact();
        long streamItemId = ContentUris.parseId(
                insertStreamItem(rawContactId, buildGenericStreamItemValues(), null));
        long streamItemPhotoId = ContentUris.parseId(
                insertStreamItemPhoto(streamItemId, buildGenericStreamItemPhotoValues(0), null));
        mActor.removePermissions("android.permission.READ_SOCIAL_STREAM");

        // Try selecting the stream item photo in various ways.
        expectSecurityException(
                "Querying all stream item photos requires social stream read permission",
                StreamItems.CONTENT_URI.buildUpon()
                        .appendPath(StreamItems.StreamItemPhotos.CONTENT_DIRECTORY).build(),
                null, null, null, null);

        expectSecurityException(
                "Querying all stream item photos requires social stream read permission",
                StreamItems.CONTENT_URI.buildUpon()
                        .appendPath(String.valueOf(streamItemId))
                        .appendPath(StreamItems.StreamItemPhotos.CONTENT_DIRECTORY)
                        .appendPath(String.valueOf(streamItemPhotoId)).build(),
                null, null, null, null);
    }

    public void testStreamItemModificationRequiresWriteSocialStreamPermission() {
        long rawContactId = createRawContact();
        long streamItemId = ContentUris.parseId(
                insertStreamItem(rawContactId, buildGenericStreamItemValues(), null));
        mActor.removePermissions("android.permission.WRITE_SOCIAL_STREAM");

        try {
            insertStreamItem(rawContactId, buildGenericStreamItemValues(), null);
            fail("Should not be able to insert to stream without write social stream permission");
        } catch (SecurityException expected) {
        }

        try {
            ContentValues values = new ContentValues();
            values.put(StreamItems.TEXT, "Goodbye world");
            mResolver.update(ContentUris.withAppendedId(StreamItems.CONTENT_URI, streamItemId),
                    values, null, null);
            fail("Should not be able to update stream without write social stream permission");
        } catch (SecurityException expected) {
        }

        try {
            mResolver.delete(ContentUris.withAppendedId(StreamItems.CONTENT_URI, streamItemId),
                    null, null);
            fail("Should not be able to delete from stream without write social stream permission");
        } catch (SecurityException expected) {
        }
    }

    public void testStreamItemPhotoModificationRequiresWriteSocialStreamPermission() {
        long rawContactId = createRawContact();
        long streamItemId = ContentUris.parseId(
                insertStreamItem(rawContactId, buildGenericStreamItemValues(), null));
        long streamItemPhotoId = ContentUris.parseId(
                insertStreamItemPhoto(streamItemId, buildGenericStreamItemPhotoValues(0), null));
        mActor.removePermissions("android.permission.WRITE_SOCIAL_STREAM");

        Uri photoUri = StreamItems.CONTENT_URI.buildUpon()
                .appendPath(String.valueOf(streamItemId))
                .appendPath(StreamItems.StreamItemPhotos.CONTENT_DIRECTORY)
                .appendPath(String.valueOf(streamItemPhotoId)).build();

        try {
            insertStreamItemPhoto(streamItemId, buildGenericStreamItemPhotoValues(1), null);
            fail("Should not be able to insert photos without write social stream permission");
        } catch (SecurityException expected) {
        }

        try {
            ContentValues values = new ContentValues();
            values.put(StreamItemPhotos.PHOTO, loadPhotoFromResource(R.drawable.galaxy,
                    PhotoSize.ORIGINAL));
            mResolver.update(photoUri, values, null, null);
            fail("Should not be able to update photos without write social stream permission");
        } catch (SecurityException expected) {
        }

        try {
            mResolver.delete(photoUri, null, null);
            fail("Should not be able to delete photos without write social stream permission");
        } catch (SecurityException expected) {
        }
    }

    public void testStatusUpdateDoesNotRequireReadOrWriteSocialStreamPermission() {
        int protocol1 = Im.PROTOCOL_GOOGLE_TALK;
        String handle1 = "test@gmail.com";
        long rawContactId = createRawContact();
        insertImHandle(rawContactId, protocol1, null, handle1);
        mActor.removePermissions("android.permission.READ_SOCIAL_STREAM");
        mActor.removePermissions("android.permission.WRITE_SOCIAL_STREAM");

        insertStatusUpdate(protocol1, null, handle1, StatusUpdates.AVAILABLE, "Green",
                StatusUpdates.CAPABILITY_HAS_CAMERA);

        mActor.addPermissions("android.permission.READ_SOCIAL_STREAM");

        ContentValues expectedValues = new ContentValues();
        expectedValues.put(StreamItems.TEXT, "Green");
        assertStoredValues(Uri.withAppendedPath(
                        ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
                        RawContacts.StreamItems.CONTENT_DIRECTORY), expectedValues);
    }

    private ContentValues buildGenericStreamItemValues() {
        ContentValues values = new ContentValues();
        values.put(StreamItems.TEXT, "Hello world");
        values.put(StreamItems.TIMESTAMP, System.currentTimeMillis());
        values.put(StreamItems.COMMENTS, "Reshared by 123 others");
        return values;
    }

    private ContentValues buildGenericStreamItemPhotoValues(int sortIndex) {
        ContentValues values = new ContentValues();
        values.put(StreamItemPhotos.SORT_INDEX, sortIndex);
        values.put(StreamItemPhotos.PHOTO,
                loadPhotoFromResource(R.drawable.earth_normal, PhotoSize.ORIGINAL));
        return values;
    }

    public void testSingleStatusUpdateRowPerContact() {
        int protocol1 = Im.PROTOCOL_GOOGLE_TALK;
        String handle1 = "test@gmail.com";

        long rawContactId1 = createRawContact();
        insertImHandle(rawContactId1, protocol1, null, handle1);

        insertStatusUpdate(protocol1, null, handle1, StatusUpdates.AVAILABLE, "Green",
                StatusUpdates.CAPABILITY_HAS_CAMERA);
        insertStatusUpdate(protocol1, null, handle1, StatusUpdates.AWAY, "Yellow",
                StatusUpdates.CAPABILITY_HAS_CAMERA);
        insertStatusUpdate(protocol1, null, handle1, StatusUpdates.INVISIBLE, "Red",
                StatusUpdates.CAPABILITY_HAS_CAMERA);

        Cursor c = queryContact(queryContactId(rawContactId1),
                new String[] {Contacts.CONTACT_PRESENCE, Contacts.CONTACT_STATUS});
        assertEquals(1, c.getCount());

        c.moveToFirst();
        assertEquals(StatusUpdates.INVISIBLE, c.getInt(0));
        assertEquals("Red", c.getString(1));
        c.close();
    }

    private void updateSendToVoicemailAndRingtone(long contactId, boolean sendToVoicemail,
            String ringtone) {
        ContentValues values = new ContentValues();
        values.put(Contacts.SEND_TO_VOICEMAIL, sendToVoicemail);
        if (ringtone != null) {
            values.put(Contacts.CUSTOM_RINGTONE, ringtone);
        }

        final Uri uri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
        int count = mResolver.update(uri, values, null, null);
        assertEquals(1, count);
    }

    private void updateSendToVoicemailAndRingtoneWithSelection(long contactId,
            boolean sendToVoicemail, String ringtone) {
        ContentValues values = new ContentValues();
        values.put(Contacts.SEND_TO_VOICEMAIL, sendToVoicemail);
        if (ringtone != null) {
            values.put(Contacts.CUSTOM_RINGTONE, ringtone);
        }

        int count = mResolver.update(Contacts.CONTENT_URI, values, Contacts._ID + "=" + contactId,
                null);
        assertEquals(1, count);
    }

    private void assertSendToVoicemailAndRingtone(long contactId, boolean expectedSendToVoicemail,
            String expectedRingtone) {
        Cursor c = queryContact(contactId);
        assertTrue(c.moveToNext());
        int sendToVoicemail = c.getInt(c.getColumnIndex(Contacts.SEND_TO_VOICEMAIL));
        assertEquals(expectedSendToVoicemail ? 1 : 0, sendToVoicemail);
        String ringtone = c.getString(c.getColumnIndex(Contacts.CUSTOM_RINGTONE));
        if (expectedRingtone == null) {
            assertNull(ringtone);
        } else {
            assertTrue(ArrayUtils.contains(expectedRingtone.split(","), ringtone));
        }
        c.close();
    }

    public void testContactVisibilityUpdateOnMembershipChange() {
        long rawContactId = createRawContact(mAccount);
        assertVisibility(rawContactId, "0");

        long visibleGroupId = createGroup(mAccount, "123", "Visible", 1);
        long invisibleGroupId = createGroup(mAccount, "567", "Invisible", 0);

        Uri membership1 = insertGroupMembership(rawContactId, visibleGroupId);
        assertVisibility(rawContactId, "1");

        Uri membership2 = insertGroupMembership(rawContactId, invisibleGroupId);
        assertVisibility(rawContactId, "1");

        mResolver.delete(membership1, null, null);
        assertVisibility(rawContactId, "0");

        ContentValues values = new ContentValues();
        values.put(GroupMembership.GROUP_ROW_ID, visibleGroupId);

        mResolver.update(membership2, values, null, null);
        assertVisibility(rawContactId, "1");
    }

    private void assertVisibility(long rawContactId, String expectedValue) {
        assertStoredValue(Contacts.CONTENT_URI, Contacts._ID + "=" + queryContactId(rawContactId),
                null, Contacts.IN_VISIBLE_GROUP, expectedValue);
    }

    public void testSupplyingBothValuesAndParameters() throws Exception {
        Account account = new Account("account 1", "type%/:1");
        Uri uri = ContactsContract.Groups.CONTENT_URI.buildUpon()
                .appendQueryParameter(ContactsContract.Groups.ACCOUNT_NAME, account.name)
                .appendQueryParameter(ContactsContract.Groups.ACCOUNT_TYPE, account.type)
                .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                .build();

        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(uri);
        builder.withValue(ContactsContract.Groups.ACCOUNT_TYPE, account.type);
        builder.withValue(ContactsContract.Groups.ACCOUNT_NAME, account.name);
        builder.withValue(ContactsContract.Groups.SYSTEM_ID, "some id");
        builder.withValue(ContactsContract.Groups.TITLE, "some name");
        builder.withValue(ContactsContract.Groups.GROUP_VISIBLE, 1);

        mResolver.applyBatch(ContactsContract.AUTHORITY, Lists.newArrayList(builder.build()));

        builder = ContentProviderOperation.newInsert(uri);
        builder.withValue(ContactsContract.Groups.ACCOUNT_TYPE, account.type + "diff");
        builder.withValue(ContactsContract.Groups.ACCOUNT_NAME, account.name);
        builder.withValue(ContactsContract.Groups.SYSTEM_ID, "some other id");
        builder.withValue(ContactsContract.Groups.TITLE, "some other name");
        builder.withValue(ContactsContract.Groups.GROUP_VISIBLE, 1);

        try {
            mResolver.applyBatch(ContactsContract.AUTHORITY, Lists.newArrayList(builder.build()));
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            // Expected
        }
    }

    public void testContentEntityIterator() {
        // create multiple contacts and check that the selected ones are returned
        long id;

        long groupId1 = createGroup(mAccount, "gsid1", "title1");
        long groupId2 = createGroup(mAccount, "gsid2", "title2");

        id = createRawContact(mAccount, RawContacts.SOURCE_ID, "c0");
        insertGroupMembership(id, "gsid1");
        insertEmail(id, "c0@email.com");
        insertPhoneNumber(id, "5551212c0");

        long c1 = id = createRawContact(mAccount, RawContacts.SOURCE_ID, "c1");
        Uri id_1_0 = insertGroupMembership(id, "gsid1");
        Uri id_1_1 = insertGroupMembership(id, "gsid2");
        Uri id_1_2 = insertEmail(id, "c1@email.com");
        Uri id_1_3 = insertPhoneNumber(id, "5551212c1");

        long c2 = id = createRawContact(mAccount, RawContacts.SOURCE_ID, "c2");
        Uri id_2_0 = insertGroupMembership(id, "gsid1");
        Uri id_2_1 = insertEmail(id, "c2@email.com");
        Uri id_2_2 = insertPhoneNumber(id, "5551212c2");

        long c3 = id = createRawContact(mAccount, RawContacts.SOURCE_ID, "c3");
        Uri id_3_0 = insertGroupMembership(id, groupId2);
        Uri id_3_1 = insertEmail(id, "c3@email.com");
        Uri id_3_2 = insertPhoneNumber(id, "5551212c3");

        EntityIterator iterator = RawContacts.newEntityIterator(mResolver.query(
                maybeAddAccountQueryParameters(RawContactsEntity.CONTENT_URI, mAccount), null,
                RawContacts.SOURCE_ID + " in ('c1', 'c2', 'c3')", null, null));
        Entity entity;
        ContentValues[] subValues;
        entity = iterator.next();
        assertEquals(c1, (long) entity.getEntityValues().getAsLong(RawContacts._ID));
        subValues = asSortedContentValuesArray(entity.getSubValues());
        assertEquals(4, subValues.length);
        assertDataRow(subValues[0], GroupMembership.CONTENT_ITEM_TYPE,
                Data._ID, id_1_0,
                GroupMembership.GROUP_ROW_ID, groupId1,
                GroupMembership.GROUP_SOURCE_ID, "gsid1");
        assertDataRow(subValues[1], GroupMembership.CONTENT_ITEM_TYPE,
                Data._ID, id_1_1,
                GroupMembership.GROUP_ROW_ID, groupId2,
                GroupMembership.GROUP_SOURCE_ID, "gsid2");
        assertDataRow(subValues[2], Email.CONTENT_ITEM_TYPE,
                Data._ID, id_1_2,
                Email.DATA, "c1@email.com");
        assertDataRow(subValues[3], Phone.CONTENT_ITEM_TYPE,
                Data._ID, id_1_3,
                Email.DATA, "5551212c1");

        entity = iterator.next();
        assertEquals(c2, (long) entity.getEntityValues().getAsLong(RawContacts._ID));
        subValues = asSortedContentValuesArray(entity.getSubValues());
        assertEquals(3, subValues.length);
        assertDataRow(subValues[0], GroupMembership.CONTENT_ITEM_TYPE,
                Data._ID, id_2_0,
                GroupMembership.GROUP_ROW_ID, groupId1,
                GroupMembership.GROUP_SOURCE_ID, "gsid1");
        assertDataRow(subValues[1], Email.CONTENT_ITEM_TYPE,
                Data._ID, id_2_1,
                Email.DATA, "c2@email.com");
        assertDataRow(subValues[2], Phone.CONTENT_ITEM_TYPE,
                Data._ID, id_2_2,
                Email.DATA, "5551212c2");

        entity = iterator.next();
        assertEquals(c3, (long) entity.getEntityValues().getAsLong(RawContacts._ID));
        subValues = asSortedContentValuesArray(entity.getSubValues());
        assertEquals(3, subValues.length);
        assertDataRow(subValues[0], GroupMembership.CONTENT_ITEM_TYPE,
                Data._ID, id_3_0,
                GroupMembership.GROUP_ROW_ID, groupId2,
                GroupMembership.GROUP_SOURCE_ID, "gsid2");
        assertDataRow(subValues[1], Email.CONTENT_ITEM_TYPE,
                Data._ID, id_3_1,
                Email.DATA, "c3@email.com");
        assertDataRow(subValues[2], Phone.CONTENT_ITEM_TYPE,
                Data._ID, id_3_2,
                Email.DATA, "5551212c3");

        assertFalse(iterator.hasNext());
        iterator.close();
    }

    public void testDataCreateUpdateDeleteByMimeType() throws Exception {
        long rawContactId = createRawContact();

        ContentValues values = new ContentValues();
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        values.put(Data.MIMETYPE, "testmimetype");
        values.put(Data.RES_PACKAGE, "oldpackage");
        values.put(Data.IS_PRIMARY, 1);
        values.put(Data.IS_SUPER_PRIMARY, 1);
        values.put(Data.DATA1, "old1");
        values.put(Data.DATA2, "old2");
        values.put(Data.DATA3, "old3");
        values.put(Data.DATA4, "old4");
        values.put(Data.DATA5, "old5");
        values.put(Data.DATA6, "old6");
        values.put(Data.DATA7, "old7");
        values.put(Data.DATA8, "old8");
        values.put(Data.DATA9, "old9");
        values.put(Data.DATA10, "old10");
        values.put(Data.DATA11, "old11");
        values.put(Data.DATA12, "old12");
        values.put(Data.DATA13, "old13");
        values.put(Data.DATA14, "old14");
        values.put(Data.DATA15, "old15");
        Uri uri = mResolver.insert(Data.CONTENT_URI, values);
        assertStoredValues(uri, values);
        assertNetworkNotified(true);

        values.clear();
        values.put(Data.RES_PACKAGE, "newpackage");
        values.put(Data.IS_PRIMARY, 0);
        values.put(Data.IS_SUPER_PRIMARY, 0);
        values.put(Data.DATA1, "new1");
        values.put(Data.DATA2, "new2");
        values.put(Data.DATA3, "new3");
        values.put(Data.DATA4, "new4");
        values.put(Data.DATA5, "new5");
        values.put(Data.DATA6, "new6");
        values.put(Data.DATA7, "new7");
        values.put(Data.DATA8, "new8");
        values.put(Data.DATA9, "new9");
        values.put(Data.DATA10, "new10");
        values.put(Data.DATA11, "new11");
        values.put(Data.DATA12, "new12");
        values.put(Data.DATA13, "new13");
        values.put(Data.DATA14, "new14");
        values.put(Data.DATA15, "new15");
        mResolver.update(Data.CONTENT_URI, values, Data.RAW_CONTACT_ID + "=" + rawContactId +
                " AND " + Data.MIMETYPE + "='testmimetype'", null);
        assertNetworkNotified(true);

        assertStoredValues(uri, values);

        int count = mResolver.delete(Data.CONTENT_URI, Data.RAW_CONTACT_ID + "=" + rawContactId
                + " AND " + Data.MIMETYPE + "='testmimetype'", null);
        assertEquals(1, count);
        assertEquals(0, getCount(Data.CONTENT_URI, Data.RAW_CONTACT_ID + "=" + rawContactId
                        + " AND " + Data.MIMETYPE + "='testmimetype'", null));
        assertNetworkNotified(true);
    }

    public void testRawContactQuery() {
        Account account1 = new Account("a", "b");
        Account account2 = new Account("c", "d");
        long rawContactId1 = createRawContact(account1);
        long rawContactId2 = createRawContact(account2);

        Uri uri1 = maybeAddAccountQueryParameters(RawContacts.CONTENT_URI, account1);
        Uri uri2 = maybeAddAccountQueryParameters(RawContacts.CONTENT_URI, account2);
        assertEquals(1, getCount(uri1, null, null));
        assertEquals(1, getCount(uri2, null, null));
        assertStoredValue(uri1, RawContacts._ID, rawContactId1) ;
        assertStoredValue(uri2, RawContacts._ID, rawContactId2) ;

        Uri rowUri1 = ContentUris.withAppendedId(uri1, rawContactId1);
        Uri rowUri2 = ContentUris.withAppendedId(uri2, rawContactId2);
        assertStoredValue(rowUri1, RawContacts._ID, rawContactId1) ;
        assertStoredValue(rowUri2, RawContacts._ID, rawContactId2) ;
    }

    public void testRawContactDeletion() {
        long rawContactId = createRawContact(mAccount);
        Uri uri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId);

        insertImHandle(rawContactId, Im.PROTOCOL_GOOGLE_TALK, null, "deleteme@android.com");
        insertStatusUpdate(Im.PROTOCOL_GOOGLE_TALK, null, "deleteme@android.com",
                StatusUpdates.AVAILABLE, null,
                StatusUpdates.CAPABILITY_HAS_CAMERA);
        long contactId = queryContactId(rawContactId);

        assertEquals(1, getCount(Uri.withAppendedPath(uri, RawContacts.Data.CONTENT_DIRECTORY),
                null, null));
        assertEquals(1, getCount(StatusUpdates.CONTENT_URI, PresenceColumns.RAW_CONTACT_ID + "="
                + rawContactId, null));

        mResolver.delete(uri, null, null);

        assertStoredValue(uri, RawContacts.DELETED, "1");
        assertNetworkNotified(true);

        Uri permanentDeletionUri = setCallerIsSyncAdapter(uri, mAccount);
        mResolver.delete(permanentDeletionUri, null, null);
        assertEquals(0, getCount(uri, null, null));
        assertEquals(0, getCount(Uri.withAppendedPath(uri, RawContacts.Data.CONTENT_DIRECTORY),
                null, null));
        assertEquals(0, getCount(StatusUpdates.CONTENT_URI, PresenceColumns.RAW_CONTACT_ID + "="
                + rawContactId, null));
        assertEquals(0, getCount(Contacts.CONTENT_URI, Contacts._ID + "=" + contactId, null));
        assertNetworkNotified(false);
    }

    public void testRawContactDeletionKeepingAggregateContact() {
        long rawContactId1 = createRawContactWithName(mAccount);
        long rawContactId2 = createRawContactWithName(mAccount);
        setAggregationException(
                AggregationExceptions.TYPE_KEEP_TOGETHER, rawContactId1, rawContactId2);

        long contactId = queryContactId(rawContactId1);

        Uri uri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId1);
        Uri permanentDeletionUri = setCallerIsSyncAdapter(uri, mAccount);
        mResolver.delete(permanentDeletionUri, null, null);
        assertEquals(0, getCount(uri, null, null));
        assertEquals(1, getCount(Contacts.CONTENT_URI, Contacts._ID + "=" + contactId, null));
    }

    public void testRawContactDeletionWithAccounts() {
        long rawContactId = createRawContact(mAccount);
        Uri uri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId);

        insertImHandle(rawContactId, Im.PROTOCOL_GOOGLE_TALK, null, "deleteme@android.com");
        insertStatusUpdate(Im.PROTOCOL_GOOGLE_TALK, null, "deleteme@android.com",
                StatusUpdates.AVAILABLE, null,
                StatusUpdates.CAPABILITY_HAS_CAMERA);
        assertEquals(1, getCount(Uri.withAppendedPath(uri, RawContacts.Data.CONTENT_DIRECTORY),
                null, null));
        assertEquals(1, getCount(StatusUpdates.CONTENT_URI, PresenceColumns.RAW_CONTACT_ID + "="
                + rawContactId, null));

        // Do not delete if we are deleting with wrong account.
        Uri deleteWithWrongAccountUri =
            RawContacts.CONTENT_URI.buildUpon()
                .appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_NAME, mAccountTwo.name)
                .appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_TYPE, mAccountTwo.type)
                .build();
        mResolver.delete(deleteWithWrongAccountUri, null, null);

        assertStoredValue(uri, RawContacts.DELETED, "0");

        // Delete if we are deleting with correct account.
        Uri deleteWithCorrectAccountUri =
            RawContacts.CONTENT_URI.buildUpon()
                .appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_NAME, mAccount.name)
                .appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_TYPE, mAccount.type)
                .build();
        mResolver.delete(deleteWithCorrectAccountUri, null, null);

        assertStoredValue(uri, RawContacts.DELETED, "1");
    }

    public void testAccountsUpdated() {
        // This is to ensure we do not delete contacts with null, null (account name, type)
        // accidentally.
        long rawContactId3 = createRawContactWithName("James", "Sullivan");
        insertPhoneNumber(rawContactId3, "5234567890");
        Uri rawContact3 = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId3);
        assertEquals(1, getCount(RawContacts.CONTENT_URI, null, null));

        ContactsProvider2 cp = (ContactsProvider2) getProvider();
        mActor.setAccounts(new Account[]{mAccount, mAccountTwo});
        cp.onAccountsUpdated(new Account[]{mAccount, mAccountTwo});
        assertEquals(1, getCount(RawContacts.CONTENT_URI, null, null));
        assertStoredValue(rawContact3, RawContacts.ACCOUNT_NAME, null);
        assertStoredValue(rawContact3, RawContacts.ACCOUNT_TYPE, null);

        long rawContactId1 = createRawContact(mAccount);
        insertEmail(rawContactId1, "account1@email.com");
        long rawContactId2 = createRawContact(mAccountTwo);
        insertEmail(rawContactId2, "account2@email.com");
        insertImHandle(rawContactId2, Im.PROTOCOL_GOOGLE_TALK, null, "deleteme@android.com");
        insertStatusUpdate(Im.PROTOCOL_GOOGLE_TALK, null, "deleteme@android.com",
                StatusUpdates.AVAILABLE, null,
                StatusUpdates.CAPABILITY_HAS_CAMERA);

        mActor.setAccounts(new Account[]{mAccount});
        cp.onAccountsUpdated(new Account[]{mAccount});
        assertEquals(2, getCount(RawContacts.CONTENT_URI, null, null));
        assertEquals(0, getCount(StatusUpdates.CONTENT_URI, PresenceColumns.RAW_CONTACT_ID + "="
                + rawContactId2, null));
    }

    public void testAccountDeletion() {
        Account readOnlyAccount = new Account("act", READ_ONLY_ACCOUNT_TYPE);
        ContactsProvider2 cp = (ContactsProvider2) getProvider();
        mActor.setAccounts(new Account[]{readOnlyAccount, mAccount});
        cp.onAccountsUpdated(new Account[]{readOnlyAccount, mAccount});

        long rawContactId1 = createRawContactWithName("John", "Doe", readOnlyAccount);
        Uri photoUri1 = insertPhoto(rawContactId1);
        long rawContactId2 = createRawContactWithName("john", "doe", mAccount);
        Uri photoUri2 = insertPhoto(rawContactId2);
        storeValue(photoUri2, Photo.IS_SUPER_PRIMARY, "1");

        assertAggregated(rawContactId1, rawContactId2);

        long contactId = queryContactId(rawContactId1);

        // The display name should come from the writable account
        assertStoredValue(Uri.withAppendedPath(
                ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId),
                Contacts.Data.CONTENT_DIRECTORY),
                Contacts.DISPLAY_NAME, "john doe");

        // The photo should be the one we marked as super-primary
        assertStoredValue(Contacts.CONTENT_URI, contactId,
                Contacts.PHOTO_ID, ContentUris.parseId(photoUri2));

        mActor.setAccounts(new Account[]{readOnlyAccount});
        // Remove the writable account
        cp.onAccountsUpdated(new Account[]{readOnlyAccount});

        // The display name should come from the remaining account
        assertStoredValue(Uri.withAppendedPath(
                ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId),
                Contacts.Data.CONTENT_DIRECTORY),
                Contacts.DISPLAY_NAME, "John Doe");

        // The photo should be the remaining one
        assertStoredValue(Contacts.CONTENT_URI, contactId,
                Contacts.PHOTO_ID, ContentUris.parseId(photoUri1));
    }

    public void testStreamItemsCleanedUpOnAccountRemoval() {
        Account doomedAccount = new Account("doom", "doom");
        Account safeAccount = mAccount;
        ContactsProvider2 cp = (ContactsProvider2) getProvider();
        mActor.setAccounts(new Account[]{doomedAccount, safeAccount});
        cp.onAccountsUpdated(new Account[]{doomedAccount, safeAccount});

        // Create a doomed raw contact, stream item, and photo.
        long doomedRawContactId = createRawContactWithName(doomedAccount);
        Uri doomedStreamItemUri =
                insertStreamItem(doomedRawContactId, buildGenericStreamItemValues(), doomedAccount);
        long doomedStreamItemId = ContentUris.parseId(doomedStreamItemUri);
        Uri doomedStreamItemPhotoUri = insertStreamItemPhoto(
                doomedStreamItemId, buildGenericStreamItemPhotoValues(0), doomedAccount);

        // Create a safe raw contact, stream item, and photo.
        long safeRawContactId = createRawContactWithName(safeAccount);
        Uri safeStreamItemUri =
                insertStreamItem(safeRawContactId, buildGenericStreamItemValues(), safeAccount);
        long safeStreamItemId = ContentUris.parseId(safeStreamItemUri);
        Uri safeStreamItemPhotoUri = insertStreamItemPhoto(
                safeStreamItemId, buildGenericStreamItemPhotoValues(0), safeAccount);
        long safeStreamItemPhotoId = ContentUris.parseId(safeStreamItemPhotoUri);

        // Remove the doomed account.
        mActor.setAccounts(new Account[]{safeAccount});
        cp.onAccountsUpdated(new Account[]{safeAccount});

        // Check that the doomed stuff has all been nuked.
        ContentValues[] noValues = new ContentValues[0];
        assertStoredValues(ContentUris.withAppendedId(RawContacts.CONTENT_URI, doomedRawContactId),
                noValues);
        assertStoredValues(doomedStreamItemUri, noValues);
        assertStoredValues(doomedStreamItemPhotoUri, noValues);

        // Check that the safe stuff lives on.
        assertStoredValue(RawContacts.CONTENT_URI, safeRawContactId, RawContacts._ID,
                safeRawContactId);
        assertStoredValue(safeStreamItemUri, StreamItems._ID, safeStreamItemId);
        assertStoredValue(safeStreamItemPhotoUri, StreamItemPhotos._ID, safeStreamItemPhotoId);
    }

    public void testContactDeletion() {
        long rawContactId1 = createRawContactWithName("John", "Doe", ACCOUNT_1);
        long rawContactId2 = createRawContactWithName("John", "Doe", ACCOUNT_2);

        long contactId = queryContactId(rawContactId1);

        mResolver.delete(ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId), null, null);

        assertStoredValue(ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId1),
                RawContacts.DELETED, "1");
        assertStoredValue(ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId2),
                RawContacts.DELETED, "1");
    }

    public void testMarkAsDirtyParameter() {
        long rawContactId = createRawContact(mAccount);
        Uri rawContactUri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId);

        Uri uri = insertStructuredName(rawContactId, "John", "Doe");
        clearDirty(rawContactUri);
        Uri updateUri = setCallerIsSyncAdapter(uri, mAccount);

        ContentValues values = new ContentValues();
        values.put(StructuredName.FAMILY_NAME, "Dough");
        mResolver.update(updateUri, values, null, null);
        assertStoredValue(uri, StructuredName.FAMILY_NAME, "Dough");
        assertDirty(rawContactUri, false);
        assertNetworkNotified(false);
    }

    public void testRawContactDirtyAndVersion() {
        final long rawContactId = createRawContact(mAccount);
        Uri uri = ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, rawContactId);
        assertDirty(uri, false);
        long version = getVersion(uri);

        ContentValues values = new ContentValues();
        values.put(ContactsContract.RawContacts.DIRTY, 0);
        values.put(ContactsContract.RawContacts.SEND_TO_VOICEMAIL, 1);
        values.put(ContactsContract.RawContacts.AGGREGATION_MODE,
                RawContacts.AGGREGATION_MODE_IMMEDIATE);
        values.put(ContactsContract.RawContacts.STARRED, 1);
        assertEquals(1, mResolver.update(uri, values, null, null));
        assertEquals(version, getVersion(uri));

        assertDirty(uri, false);
        assertNetworkNotified(false);

        Uri emailUri = insertEmail(rawContactId, "goo@woo.com");
        assertDirty(uri, true);
        assertNetworkNotified(true);
        ++version;
        assertEquals(version, getVersion(uri));
        clearDirty(uri);

        values = new ContentValues();
        values.put(Email.DATA, "goo@hoo.com");
        mResolver.update(emailUri, values, null, null);
        assertDirty(uri, true);
        assertNetworkNotified(true);
        ++version;
        assertEquals(version, getVersion(uri));
        clearDirty(uri);

        mResolver.delete(emailUri, null, null);
        assertDirty(uri, true);
        assertNetworkNotified(true);
        ++version;
        assertEquals(version, getVersion(uri));
    }

    public void testRawContactClearDirty() {
        final long rawContactId = createRawContact(mAccount);
        Uri uri = ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI,
                rawContactId);
        long version = getVersion(uri);
        insertEmail(rawContactId, "goo@woo.com");
        assertDirty(uri, true);
        version++;
        assertEquals(version, getVersion(uri));

        clearDirty(uri);
        assertDirty(uri, false);
        assertEquals(version, getVersion(uri));
    }

    public void testRawContactDeletionSetsDirty() {
        final long rawContactId = createRawContact(mAccount);
        Uri uri = ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI,
                rawContactId);
        long version = getVersion(uri);
        clearDirty(uri);
        assertDirty(uri, false);

        mResolver.delete(uri, null, null);
        assertStoredValue(uri, RawContacts.DELETED, "1");
        assertDirty(uri, true);
        assertNetworkNotified(true);
        version++;
        assertEquals(version, getVersion(uri));
    }

    public void testDeleteContactWithoutName() {
        Uri rawContactUri = mResolver.insert(RawContacts.CONTENT_URI, new ContentValues());
        long rawContactId = ContentUris.parseId(rawContactUri);

        Uri phoneUri = insertPhoneNumber(rawContactId, "555-123-45678", true);

        long contactId = queryContactId(rawContactId);
        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
        Uri lookupUri = Contacts.getLookupUri(mResolver, contactUri);

        int numDeleted = mResolver.delete(lookupUri, null, null);
        assertEquals(1, numDeleted);
    }

    public void testDeleteContactWithoutAnyData() {
        Uri rawContactUri = mResolver.insert(RawContacts.CONTENT_URI, new ContentValues());
        long rawContactId = ContentUris.parseId(rawContactUri);

        long contactId = queryContactId(rawContactId);
        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
        Uri lookupUri = Contacts.getLookupUri(mResolver, contactUri);

        int numDeleted = mResolver.delete(lookupUri, null, null);
        assertEquals(1, numDeleted);
    }

    public void testDeleteContactWithEscapedUri() {
        ContentValues values = new ContentValues();
        values.put(RawContacts.SOURCE_ID, "!@#$%^&*()_+=-/.,<>?;'\":[]}{\\|`~");
        Uri rawContactUri = mResolver.insert(RawContacts.CONTENT_URI, values);
        long rawContactId = ContentUris.parseId(rawContactUri);

        long contactId = queryContactId(rawContactId);
        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
        Uri lookupUri = Contacts.getLookupUri(mResolver, contactUri);
        assertEquals(1, mResolver.delete(lookupUri, null, null));
    }

    public void testQueryContactWithEscapedUri() {
        ContentValues values = new ContentValues();
        values.put(RawContacts.SOURCE_ID, "!@#$%^&*()_+=-/.,<>?;'\":[]}{\\|`~");
        Uri rawContactUri = mResolver.insert(RawContacts.CONTENT_URI, values);
        long rawContactId = ContentUris.parseId(rawContactUri);

        long contactId = queryContactId(rawContactId);
        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
        Uri lookupUri = Contacts.getLookupUri(mResolver, contactUri);
        Cursor c = mResolver.query(lookupUri, null, null, null, "");
        assertEquals(1, c.getCount());
        c.close();
    }

    public void testGetPhotoUri() {
        ContentValues values = new ContentValues();
        Uri rawContactUri = mResolver.insert(RawContacts.CONTENT_URI, values);
        long rawContactId = ContentUris.parseId(rawContactUri);
        insertStructuredName(rawContactId, "John", "Doe");
        long dataId = ContentUris.parseId(insertPhoto(rawContactId, R.drawable.earth_normal));
        long photoFileId = getStoredLongValue(Data.CONTENT_URI, Data._ID + "=?",
                new String[]{String.valueOf(dataId)}, Photo.PHOTO_FILE_ID);
        String photoUri = ContentUris.withAppendedId(DisplayPhoto.CONTENT_URI, photoFileId)
                .toString();

        assertStoredValue(
                ContentUris.withAppendedId(Contacts.CONTENT_URI, queryContactId(rawContactId)),
                Contacts.PHOTO_URI, photoUri);
    }

    public void testGetPhotoViaLookupUri() throws IOException {
        long rawContactId = createRawContact();
        long contactId = queryContactId(rawContactId);
        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
        Uri lookupUri = Contacts.getLookupUri(mResolver, contactUri);
        String lookupKey = lookupUri.getPathSegments().get(2);
        insertPhoto(rawContactId, R.drawable.earth_small);
        byte[] thumbnail = loadPhotoFromResource(R.drawable.earth_small, PhotoSize.THUMBNAIL);

        // Two forms of lookup key URIs should be valid - one with the contact ID, one without.
        Uri photoLookupUriWithId = Uri.withAppendedPath(lookupUri, "photo");
        Uri photoLookupUriWithoutId = Contacts.CONTENT_LOOKUP_URI.buildUpon()
                .appendPath(lookupKey).appendPath("photo").build();

        // Try retrieving as a data record.
        ContentValues values = new ContentValues();
        values.put(Photo.PHOTO, thumbnail);
        assertStoredValues(photoLookupUriWithId, values);
        assertStoredValues(photoLookupUriWithoutId, values);

        // Try opening as an input stream.
        assertInputStreamContent(thumbnail, mResolver.openInputStream(photoLookupUriWithId));
        assertInputStreamContent(thumbnail, mResolver.openInputStream(photoLookupUriWithoutId));
    }

    public void testInputStreamForPhoto() throws Exception {
        long rawContactId = createRawContact();
        long contactId = queryContactId(rawContactId);
        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
        insertPhoto(rawContactId);
        Uri photoUri = Uri.parse(getStoredValue(contactUri, Contacts.PHOTO_URI));
        Uri photoThumbnailUri = Uri.parse(getStoredValue(contactUri, Contacts.PHOTO_THUMBNAIL_URI));

        assertInputStreamContent(loadTestPhoto(PhotoSize.DISPLAY_PHOTO),
                mResolver.openInputStream(photoUri));
        assertInputStreamContent(loadTestPhoto(PhotoSize.THUMBNAIL),
                mResolver.openInputStream(photoThumbnailUri));
    }

    private static void assertInputStreamContent(byte[] expected, InputStream is)
            throws IOException {
        try {
            byte[] observed = new byte[expected.length];
            int count = is.read(observed);
            assertEquals(expected.length, count);
            assertEquals(-1, is.read());
            MoreAsserts.assertEquals(expected, observed);
        } finally {
            is.close();
        }
    }

    public void testSuperPrimaryPhoto() {
        long rawContactId1 = createRawContact(new Account("a", "a"));
        Uri photoUri1 = insertPhoto(rawContactId1, R.drawable.earth_normal);
        long photoId1 = ContentUris.parseId(photoUri1);

        long rawContactId2 = createRawContact(new Account("b", "b"));
        Uri photoUri2 = insertPhoto(rawContactId2, R.drawable.earth_normal);
        long photoId2 = ContentUris.parseId(photoUri2);

        setAggregationException(AggregationExceptions.TYPE_KEEP_TOGETHER,
                rawContactId1, rawContactId2);

        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI,
                queryContactId(rawContactId1));

        long photoFileId1 = getStoredLongValue(Data.CONTENT_URI, Data._ID + "=?",
                new String[]{String.valueOf(photoId1)}, Photo.PHOTO_FILE_ID);
        String photoUri = ContentUris.withAppendedId(DisplayPhoto.CONTENT_URI, photoFileId1)
                .toString();
        assertStoredValue(contactUri, Contacts.PHOTO_ID, photoId1);
        assertStoredValue(contactUri, Contacts.PHOTO_URI, photoUri);

        setAggregationException(AggregationExceptions.TYPE_KEEP_SEPARATE,
                rawContactId1, rawContactId2);

        ContentValues values = new ContentValues();
        values.put(Data.IS_SUPER_PRIMARY, 1);
        mResolver.update(photoUri2, values, null, null);

        setAggregationException(AggregationExceptions.TYPE_KEEP_TOGETHER,
                rawContactId1, rawContactId2);
        contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI,
                queryContactId(rawContactId1));
        assertStoredValue(contactUri, Contacts.PHOTO_ID, photoId2);

        mResolver.update(photoUri1, values, null, null);
        assertStoredValue(contactUri, Contacts.PHOTO_ID, photoId1);
    }

    public void testUpdatePhoto() {
        ContentValues values = new ContentValues();
        Uri rawContactUri = mResolver.insert(RawContacts.CONTENT_URI, values);
        long rawContactId = ContentUris.parseId(rawContactUri);
        insertStructuredName(rawContactId, "John", "Doe");

        Uri twigUri = Uri.withAppendedPath(ContentUris.withAppendedId(Contacts.CONTENT_URI,
                queryContactId(rawContactId)), Contacts.Photo.CONTENT_DIRECTORY);

        values.clear();
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        values.put(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
        values.putNull(Photo.PHOTO);
        Uri dataUri = mResolver.insert(Data.CONTENT_URI, values);
        long photoId = ContentUris.parseId(dataUri);

        assertEquals(0, getCount(twigUri, null, null));

        values.clear();
        values.put(Photo.PHOTO, loadTestPhoto());
        mResolver.update(dataUri, values, null, null);
        assertNetworkNotified(true);

        long twigId = getStoredLongValue(twigUri, Data._ID);
        assertEquals(photoId, twigId);
    }

    public void testUpdateRawContactDataPhoto() {
        // setup a contact with a null photo
        ContentValues values = new ContentValues();
        Uri rawContactUri = mResolver.insert(RawContacts.CONTENT_URI, values);
        long rawContactId = ContentUris.parseId(rawContactUri);

        // setup a photo
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        values.put(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
        values.putNull(Photo.PHOTO);

        // try to do an update before insert should return count == 0
        Uri dataUri = Uri.withAppendedPath(
                ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
                RawContacts.Data.CONTENT_DIRECTORY);
        assertEquals(0, mResolver.update(dataUri, values, Data.MIMETYPE + "=?",
                new String[] {Photo.CONTENT_ITEM_TYPE}));

        mResolver.insert(Data.CONTENT_URI, values);

        // save a photo to the db
        values.clear();
        values.put(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
        values.put(Photo.PHOTO, loadTestPhoto());
        assertEquals(1, mResolver.update(dataUri, values, Data.MIMETYPE + "=?",
                new String[] {Photo.CONTENT_ITEM_TYPE}));

        // verify the photo
        Cursor storedPhoto = mResolver.query(dataUri, new String[] {Photo.PHOTO},
                Data.MIMETYPE + "=?", new String[] {Photo.CONTENT_ITEM_TYPE}, null);
        storedPhoto.moveToFirst();
        MoreAsserts.assertEquals(loadTestPhoto(PhotoSize.THUMBNAIL), storedPhoto.getBlob(0));
        storedPhoto.close();
    }

    public void testOpenDisplayPhotoForContactId() throws IOException {
        long rawContactId = createRawContactWithName();
        long contactId = queryContactId(rawContactId);
        insertPhoto(rawContactId, R.drawable.earth_normal);
        Uri photoUri = Contacts.CONTENT_URI.buildUpon()
                .appendPath(String.valueOf(contactId))
                .appendPath(Contacts.Photo.DISPLAY_PHOTO).build();
        assertInputStreamContent(
                loadPhotoFromResource(R.drawable.earth_normal, PhotoSize.DISPLAY_PHOTO),
                mResolver.openInputStream(photoUri));
    }

    public void testOpenDisplayPhotoForContactLookupKey() throws IOException {
        long rawContactId = createRawContactWithName();
        long contactId = queryContactId(rawContactId);
        String lookupKey = queryLookupKey(contactId);
        insertPhoto(rawContactId, R.drawable.earth_normal);
        Uri photoUri = Contacts.CONTENT_LOOKUP_URI.buildUpon()
                .appendPath(lookupKey)
                .appendPath(Contacts.Photo.DISPLAY_PHOTO).build();
        assertInputStreamContent(
                loadPhotoFromResource(R.drawable.earth_normal, PhotoSize.DISPLAY_PHOTO),
                mResolver.openInputStream(photoUri));
    }

    public void testOpenDisplayPhotoForContactLookupKeyAndId() throws IOException {
        long rawContactId = createRawContactWithName();
        long contactId = queryContactId(rawContactId);
        String lookupKey = queryLookupKey(contactId);
        insertPhoto(rawContactId, R.drawable.earth_normal);
        Uri photoUri = Contacts.CONTENT_LOOKUP_URI.buildUpon()
                .appendPath(lookupKey)
                .appendPath(String.valueOf(contactId))
                .appendPath(Contacts.Photo.DISPLAY_PHOTO).build();
        assertInputStreamContent(
                loadPhotoFromResource(R.drawable.earth_normal, PhotoSize.DISPLAY_PHOTO),
                mResolver.openInputStream(photoUri));
    }

    public void testOpenDisplayPhotoForRawContactId() throws IOException {
        long rawContactId = createRawContactWithName();
        insertPhoto(rawContactId, R.drawable.earth_normal);
        Uri photoUri = RawContacts.CONTENT_URI.buildUpon()
                .appendPath(String.valueOf(rawContactId))
                .appendPath(RawContacts.DisplayPhoto.CONTENT_DIRECTORY).build();
        assertInputStreamContent(
                loadPhotoFromResource(R.drawable.earth_normal, PhotoSize.DISPLAY_PHOTO),
                mResolver.openInputStream(photoUri));
    }

    public void testOpenDisplayPhotoByPhotoUri() throws IOException {
        long rawContactId = createRawContactWithName();
        long contactId = queryContactId(rawContactId);
        insertPhoto(rawContactId, R.drawable.earth_normal);

        // Get the photo URI out and check the content.
        String photoUri = getStoredValue(
                ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId),
                Contacts.PHOTO_URI);
        assertInputStreamContent(
                loadPhotoFromResource(R.drawable.earth_normal, PhotoSize.DISPLAY_PHOTO),
                mResolver.openInputStream(Uri.parse(photoUri)));
    }

    public void testPhotoUriForDisplayPhoto() {
        long rawContactId = createRawContactWithName();
        long contactId = queryContactId(rawContactId);

        // Photo being inserted is larger than a thumbnail, so it will be stored as a file.
        long dataId = ContentUris.parseId(insertPhoto(rawContactId, R.drawable.earth_normal));
        String photoFileId = getStoredValue(ContentUris.withAppendedId(Data.CONTENT_URI, dataId),
                Photo.PHOTO_FILE_ID);
        String photoUri = getStoredValue(
                ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId),
                Contacts.PHOTO_URI);

        // Check that the photo URI differs from the thumbnail.
        String thumbnailUri = getStoredValue(
                ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId),
                Contacts.PHOTO_THUMBNAIL_URI);
        assertFalse(photoUri.equals(thumbnailUri));

        // URI should be of the form display_photo/ID
        assertEquals(Uri.withAppendedPath(DisplayPhoto.CONTENT_URI, photoFileId).toString(),
                photoUri);
    }

    public void testPhotoUriForThumbnailPhoto() throws IOException {
        long rawContactId = createRawContactWithName();
        long contactId = queryContactId(rawContactId);

        // Photo being inserted is a thumbnail, so it will only be stored in a BLOB.  The photo URI
        // will fall back to the thumbnail URI.
        insertPhoto(rawContactId, R.drawable.earth_small);
        String photoUri = getStoredValue(
                ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId),
                Contacts.PHOTO_URI);

        // Check that the photo URI is equal to the thumbnail URI.
        String thumbnailUri = getStoredValue(
                ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId),
                Contacts.PHOTO_THUMBNAIL_URI);
        assertEquals(photoUri, thumbnailUri);

        // URI should be of the form contacts/ID/photo
        assertEquals(Uri.withAppendedPath(
                ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId),
                Contacts.Photo.CONTENT_DIRECTORY).toString(),
                photoUri);

        // Loading the photo URI content should get the thumbnail.
        assertInputStreamContent(
                loadPhotoFromResource(R.drawable.earth_small, PhotoSize.THUMBNAIL),
                mResolver.openInputStream(Uri.parse(photoUri)));
    }

    public void testWriteNewPhotoToAssetFile() throws Exception {
        long rawContactId = createRawContactWithName();
        long contactId = queryContactId(rawContactId);

        // Load in a huge photo.
        final byte[] originalPhoto = loadPhotoFromResource(
                R.drawable.earth_huge, PhotoSize.ORIGINAL);

        // Write it out.
        final Uri writeablePhotoUri = RawContacts.CONTENT_URI.buildUpon()
                .appendPath(String.valueOf(rawContactId))
                .appendPath(RawContacts.DisplayPhoto.CONTENT_DIRECTORY).build();
        writePhotoAsync(writeablePhotoUri, originalPhoto);

        // Check that the display photo and thumbnail have been set.
        String photoUri = null;
        for (int i = 0; i < 10 && photoUri == null; i++) {
            // Wait a tick for the photo processing to occur.
            Thread.sleep(100);
            photoUri = getStoredValue(
                ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId),
                Contacts.PHOTO_URI);
        }

        assertFalse(TextUtils.isEmpty(photoUri));
        String thumbnailUri = getStoredValue(
                ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId),
                Contacts.PHOTO_THUMBNAIL_URI);
        assertFalse(TextUtils.isEmpty(thumbnailUri));
        assertNotSame(photoUri, thumbnailUri);

        // Check the content of the display photo and thumbnail.
        assertInputStreamContent(
                loadPhotoFromResource(R.drawable.earth_huge, PhotoSize.DISPLAY_PHOTO),
                mResolver.openInputStream(Uri.parse(photoUri)));
        assertInputStreamContent(
                loadPhotoFromResource(R.drawable.earth_huge, PhotoSize.THUMBNAIL),
                mResolver.openInputStream(Uri.parse(thumbnailUri)));
    }

    public void testWriteUpdatedPhotoToAssetFile() throws Exception {
        long rawContactId = createRawContactWithName();
        long contactId = queryContactId(rawContactId);

        // Insert a large photo first.
        insertPhoto(rawContactId, R.drawable.earth_large);
        String largeEarthPhotoUri = getStoredValue(
                ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId), Contacts.PHOTO_URI);

        // Load in a huge photo.
        byte[] originalPhoto = loadPhotoFromResource(R.drawable.earth_huge, PhotoSize.ORIGINAL);

        // Write it out.
        Uri writeablePhotoUri = RawContacts.CONTENT_URI.buildUpon()
                .appendPath(String.valueOf(rawContactId))
                .appendPath(RawContacts.DisplayPhoto.CONTENT_DIRECTORY).build();
        writePhotoAsync(writeablePhotoUri, originalPhoto);

        // Allow a second for processing to occur.
        Thread.sleep(1000);

        // Check that the display photo URI has been modified.
        String hugeEarthPhotoUri = getStoredValue(
                ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId), Contacts.PHOTO_URI);
        assertFalse(hugeEarthPhotoUri.equals(largeEarthPhotoUri));

        // Check the content of the display photo and thumbnail.
        String hugeEarthThumbnailUri = getStoredValue(
                ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId),
                Contacts.PHOTO_THUMBNAIL_URI);
        assertInputStreamContent(
                loadPhotoFromResource(R.drawable.earth_huge, PhotoSize.DISPLAY_PHOTO),
                mResolver.openInputStream(Uri.parse(hugeEarthPhotoUri)));
        assertInputStreamContent(
                loadPhotoFromResource(R.drawable.earth_huge, PhotoSize.THUMBNAIL),
                mResolver.openInputStream(Uri.parse(hugeEarthThumbnailUri)));

    }

    private void writePhotoAsync(final Uri uri, final byte[] photoBytes) throws Exception {
        AsyncTask<Object, Object, Object> task = new AsyncTask<Object, Object, Object>() {
            @Override
            protected Object doInBackground(Object... params) {
                OutputStream os;
                try {
                    os = mResolver.openOutputStream(uri, "rw");
                    os.write(photoBytes);
                    os.close();
                    return null;
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }
        };
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Object[])null).get();
    }

    public void testPhotoDimensionLimits() {
        ContentValues values = new ContentValues();
        values.put(DisplayPhoto.DISPLAY_MAX_DIM, 256);
        values.put(DisplayPhoto.THUMBNAIL_MAX_DIM, 96);
        assertStoredValues(DisplayPhoto.CONTENT_MAX_DIMENSIONS_URI, values);
    }

    public void testPhotoStoreCleanup() throws IOException {
        SynchronousContactsProvider2 provider = (SynchronousContactsProvider2) mActor.provider;
        PhotoStore photoStore = provider.getPhotoStore();

        // Trigger an initial cleanup so another one won't happen while we're running this test.
        provider.cleanupPhotoStore();

        // Insert a couple of contacts with photos.
        long rawContactId1 = createRawContactWithName();
        long contactId1 = queryContactId(rawContactId1);
        long dataId1 = ContentUris.parseId(insertPhoto(rawContactId1, R.drawable.earth_normal));
        long photoFileId1 =
                getStoredLongValue(ContentUris.withAppendedId(Data.CONTENT_URI, dataId1),
                        Photo.PHOTO_FILE_ID);

        long rawContactId2 = createRawContactWithName();
        long contactId2 = queryContactId(rawContactId2);
        long dataId2 = ContentUris.parseId(insertPhoto(rawContactId2, R.drawable.earth_normal));
        long photoFileId2 =
                getStoredLongValue(ContentUris.withAppendedId(Data.CONTENT_URI, dataId2),
                        Photo.PHOTO_FILE_ID);

        // Update the second raw contact with a different photo.
        ContentValues values = new ContentValues();
        values.put(Data.RAW_CONTACT_ID, rawContactId2);
        values.put(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
        values.put(Photo.PHOTO, loadPhotoFromResource(R.drawable.earth_huge, PhotoSize.ORIGINAL));
        assertEquals(1, mResolver.update(Data.CONTENT_URI, values, Data._ID + "=?",
                new String[]{String.valueOf(dataId2)}));
        long replacementPhotoFileId =
                getStoredLongValue(ContentUris.withAppendedId(Data.CONTENT_URI, dataId2),
                        Photo.PHOTO_FILE_ID);

        // Insert a third raw contact that has a bogus photo file ID.
        long bogusFileId = 1234567;
        long rawContactId3 = createRawContactWithName();
        long contactId3 = queryContactId(rawContactId3);
        values.clear();
        values.put(Data.RAW_CONTACT_ID, rawContactId3);
        values.put(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
        values.put(Photo.PHOTO, loadPhotoFromResource(R.drawable.earth_normal,
                PhotoSize.THUMBNAIL));
        values.put(Photo.PHOTO_FILE_ID, bogusFileId);
        values.put(DataRowHandlerForPhoto.SKIP_PROCESSING_KEY, true);
        mResolver.insert(Data.CONTENT_URI, values);

        // Insert a fourth raw contact with a stream item that has a photo, then remove that photo
        // from the photo store.
        Account socialAccount = new Account("social", "social");
        long rawContactId4 = createRawContactWithName(socialAccount);
        Uri streamItemUri =
                insertStreamItem(rawContactId4, buildGenericStreamItemValues(), socialAccount);
        long streamItemId = ContentUris.parseId(streamItemUri);
        Uri streamItemPhotoUri = insertStreamItemPhoto(
                streamItemId, buildGenericStreamItemPhotoValues(0), socialAccount);
        long streamItemPhotoFileId = getStoredLongValue(streamItemPhotoUri,
                StreamItemPhotos.PHOTO_FILE_ID);
        photoStore.remove(streamItemPhotoFileId);

        // Also insert a bogus photo that nobody is using.
        long bogusPhotoId = photoStore.insert(new PhotoProcessor(loadPhotoFromResource(
                R.drawable.earth_huge, PhotoSize.ORIGINAL), 256, 96));

        // Manually trigger another cleanup in the provider.
        provider.cleanupPhotoStore();

        // The following things should have happened.

        // 1. Raw contact 1 and its photo remain unaffected.
        assertEquals(photoFileId1, (long) getStoredLongValue(
                ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId1),
                Contacts.PHOTO_FILE_ID));

        // 2. Raw contact 2 retains its new photo.  The old one is deleted from the photo store.
        assertEquals(replacementPhotoFileId, (long) getStoredLongValue(
                ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId2),
                Contacts.PHOTO_FILE_ID));
        assertNull(photoStore.get(photoFileId2));

        // 3. Raw contact 3 should have its photo file reference cleared.
        assertNull(getStoredValue(
                ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId3),
                Contacts.PHOTO_FILE_ID));

        // 4. The bogus photo that nobody was using should be cleared from the photo store.
        assertNull(photoStore.get(bogusPhotoId));

        // 5. The bogus stream item photo should be cleared from the stream item.
        assertStoredValues(Uri.withAppendedPath(
                ContentUris.withAppendedId(StreamItems.CONTENT_URI, streamItemId),
                StreamItems.StreamItemPhotos.CONTENT_DIRECTORY),
                new ContentValues[0]);
    }

    public void testPhotoStoreCleanupForProfile() {
        SynchronousContactsProvider2 provider = (SynchronousContactsProvider2) mActor.provider;
        PhotoStore profilePhotoStore = provider.getProfilePhotoStore();

        // Trigger an initial cleanup so another one won't happen while we're running this test.
        provider.switchToProfileMode();
        provider.cleanupPhotoStore();

        // Create the profile contact and add a photo.
        Account socialAccount = new Account("social", "social");
        ContentValues values = new ContentValues();
        values.put(RawContacts.ACCOUNT_NAME, socialAccount.name);
        values.put(RawContacts.ACCOUNT_TYPE, socialAccount.type);
        long profileRawContactId = createBasicProfileContact(values);
        long profileContactId = queryContactId(profileRawContactId);
        long dataId = ContentUris.parseId(
                insertPhoto(profileRawContactId, R.drawable.earth_normal));
        long profilePhotoFileId =
                getStoredLongValue(ContentUris.withAppendedId(Data.CONTENT_URI, dataId),
                        Photo.PHOTO_FILE_ID);

        // Also add a stream item with a photo.
        Uri streamItemUri =
                insertStreamItem(profileRawContactId, buildGenericStreamItemValues(),
                        socialAccount);
        long streamItemId = ContentUris.parseId(streamItemUri);
        Uri streamItemPhotoUri = insertStreamItemPhoto(
                streamItemId, buildGenericStreamItemPhotoValues(0), socialAccount);
        long streamItemPhotoFileId = getStoredLongValue(streamItemPhotoUri,
                StreamItemPhotos.PHOTO_FILE_ID);

        // Remove the stream item photo and the profile photo.
        profilePhotoStore.remove(profilePhotoFileId);
        profilePhotoStore.remove(streamItemPhotoFileId);

        // Manually trigger another cleanup in the provider.
        provider.switchToProfileMode();
        provider.cleanupPhotoStore();

        // The following things should have happened.

        // The stream item photo should have been removed.
        assertStoredValues(Uri.withAppendedPath(
                ContentUris.withAppendedId(StreamItems.CONTENT_URI, streamItemId),
                StreamItems.StreamItemPhotos.CONTENT_DIRECTORY),
                new ContentValues[0]);

        // The profile photo should have been cleared.
        assertNull(getStoredValue(
                ContentUris.withAppendedId(Contacts.CONTENT_URI, profileContactId),
                Contacts.PHOTO_FILE_ID));

    }

    public void testOverwritePhotoWithThumbnail() throws IOException {
        long rawContactId = createRawContactWithName();
        long contactId = queryContactId(rawContactId);
        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);

        // Write a regular-size photo.
        long dataId = ContentUris.parseId(insertPhoto(rawContactId, R.drawable.earth_normal));
        Long photoFileId = getStoredLongValue(contactUri, Contacts.PHOTO_FILE_ID);
        assertTrue(photoFileId != null && photoFileId > 0);

        // Now overwrite the photo with a thumbnail-sized photo.
        ContentValues update = new ContentValues();
        update.put(Photo.PHOTO, loadPhotoFromResource(R.drawable.earth_small, PhotoSize.ORIGINAL));
        mResolver.update(ContentUris.withAppendedId(Data.CONTENT_URI, dataId), update, null, null);

        // Photo file ID should have been nulled out, and the photo URI should be the same as the
        // thumbnail URI.
        assertNull(getStoredValue(contactUri, Contacts.PHOTO_FILE_ID));
        String photoUri = getStoredValue(contactUri, Contacts.PHOTO_URI);
        String thumbnailUri = getStoredValue(contactUri, Contacts.PHOTO_THUMBNAIL_URI);
        assertEquals(photoUri, thumbnailUri);

        // Retrieving the photo URI should get the thumbnail content.
        assertInputStreamContent(loadPhotoFromResource(R.drawable.earth_small, PhotoSize.THUMBNAIL),
                mResolver.openInputStream(Uri.parse(photoUri)));
    }

    public void testUpdateRawContactSetStarred() {
        long rawContactId1 = createRawContactWithName();
        Uri rawContactUri1 = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId1);
        long rawContactId2 = createRawContactWithName();
        Uri rawContactUri2 = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId2);
        setAggregationException(
                AggregationExceptions.TYPE_KEEP_TOGETHER, rawContactId1, rawContactId2);

        long contactId = queryContactId(rawContactId1);
        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
        assertStoredValue(contactUri, Contacts.STARRED, "0");

        ContentValues values = new ContentValues();
        values.put(RawContacts.STARRED, "1");

        mResolver.update(rawContactUri1, values, null, null);

        assertStoredValue(rawContactUri1, RawContacts.STARRED, "1");
        assertStoredValue(rawContactUri2, RawContacts.STARRED, "0");
        assertStoredValue(contactUri, Contacts.STARRED, "1");

        values.put(RawContacts.STARRED, "0");
        mResolver.update(rawContactUri1, values, null, null);

        assertStoredValue(rawContactUri1, RawContacts.STARRED, "0");
        assertStoredValue(rawContactUri2, RawContacts.STARRED, "0");
        assertStoredValue(contactUri, Contacts.STARRED, "0");

        values.put(Contacts.STARRED, "1");
        mResolver.update(contactUri, values, null, null);

        assertStoredValue(rawContactUri1, RawContacts.STARRED, "1");
        assertStoredValue(rawContactUri2, RawContacts.STARRED, "1");
        assertStoredValue(contactUri, Contacts.STARRED, "1");
    }

    public void testSetAndClearSuperPrimaryEmail() {
        long rawContactId1 = createRawContact(new Account("a", "a"));
        Uri mailUri11 = insertEmail(rawContactId1, "test1@domain1.com");
        Uri mailUri12 = insertEmail(rawContactId1, "test2@domain1.com");

        long rawContactId2 = createRawContact(new Account("b", "b"));
        Uri mailUri21 = insertEmail(rawContactId2, "test1@domain2.com");
        Uri mailUri22 = insertEmail(rawContactId2, "test2@domain2.com");

        assertStoredValue(mailUri11, Data.IS_PRIMARY, 0);
        assertStoredValue(mailUri11, Data.IS_SUPER_PRIMARY, 0);
        assertStoredValue(mailUri12, Data.IS_PRIMARY, 0);
        assertStoredValue(mailUri12, Data.IS_SUPER_PRIMARY, 0);
        assertStoredValue(mailUri21, Data.IS_PRIMARY, 0);
        assertStoredValue(mailUri21, Data.IS_SUPER_PRIMARY, 0);
        assertStoredValue(mailUri22, Data.IS_PRIMARY, 0);
        assertStoredValue(mailUri22, Data.IS_SUPER_PRIMARY, 0);

        // Set super primary on the first pair, primary on the second
        {
            ContentValues values = new ContentValues();
            values.put(Data.IS_SUPER_PRIMARY, 1);
            mResolver.update(mailUri11, values, null, null);
        }
        {
            ContentValues values = new ContentValues();
            values.put(Data.IS_SUPER_PRIMARY, 1);
            mResolver.update(mailUri22, values, null, null);
        }

        assertStoredValue(mailUri11, Data.IS_PRIMARY, 1);
        assertStoredValue(mailUri11, Data.IS_SUPER_PRIMARY, 1);
        assertStoredValue(mailUri12, Data.IS_PRIMARY, 0);
        assertStoredValue(mailUri12, Data.IS_SUPER_PRIMARY, 0);
        assertStoredValue(mailUri21, Data.IS_PRIMARY, 0);
        assertStoredValue(mailUri21, Data.IS_SUPER_PRIMARY, 0);
        assertStoredValue(mailUri22, Data.IS_PRIMARY, 1);
        assertStoredValue(mailUri22, Data.IS_SUPER_PRIMARY, 1);

        // Clear primary on the first pair, make sure second is not affected and super_primary is
        // also cleared
        {
            ContentValues values = new ContentValues();
            values.put(Data.IS_PRIMARY, 0);
            mResolver.update(mailUri11, values, null, null);
        }

        assertStoredValue(mailUri11, Data.IS_PRIMARY, 0);
        assertStoredValue(mailUri11, Data.IS_SUPER_PRIMARY, 0);
        assertStoredValue(mailUri12, Data.IS_PRIMARY, 0);
        assertStoredValue(mailUri12, Data.IS_SUPER_PRIMARY, 0);
        assertStoredValue(mailUri21, Data.IS_PRIMARY, 0);
        assertStoredValue(mailUri21, Data.IS_SUPER_PRIMARY, 0);
        assertStoredValue(mailUri22, Data.IS_PRIMARY, 1);
        assertStoredValue(mailUri22, Data.IS_SUPER_PRIMARY, 1);

        // Ensure that we can only clear super_primary, if we specify the correct data row
        {
            ContentValues values = new ContentValues();
            values.put(Data.IS_SUPER_PRIMARY, 0);
            mResolver.update(mailUri21, values, null, null);
        }

        assertStoredValue(mailUri21, Data.IS_PRIMARY, 0);
        assertStoredValue(mailUri21, Data.IS_SUPER_PRIMARY, 0);
        assertStoredValue(mailUri22, Data.IS_PRIMARY, 1);
        assertStoredValue(mailUri22, Data.IS_SUPER_PRIMARY, 1);

        // Ensure that we can only clear primary, if we specify the correct data row
        {
            ContentValues values = new ContentValues();
            values.put(Data.IS_PRIMARY, 0);
            mResolver.update(mailUri21, values, null, null);
        }

        assertStoredValue(mailUri21, Data.IS_PRIMARY, 0);
        assertStoredValue(mailUri21, Data.IS_SUPER_PRIMARY, 0);
        assertStoredValue(mailUri22, Data.IS_PRIMARY, 1);
        assertStoredValue(mailUri22, Data.IS_SUPER_PRIMARY, 1);

        // Now clear super-primary for real
        {
            ContentValues values = new ContentValues();
            values.put(Data.IS_SUPER_PRIMARY, 0);
            mResolver.update(mailUri22, values, null, null);
        }

        assertStoredValue(mailUri11, Data.IS_PRIMARY, 0);
        assertStoredValue(mailUri11, Data.IS_SUPER_PRIMARY, 0);
        assertStoredValue(mailUri12, Data.IS_PRIMARY, 0);
        assertStoredValue(mailUri12, Data.IS_SUPER_PRIMARY, 0);
        assertStoredValue(mailUri21, Data.IS_PRIMARY, 0);
        assertStoredValue(mailUri21, Data.IS_SUPER_PRIMARY, 0);
        assertStoredValue(mailUri22, Data.IS_PRIMARY, 1);
        assertStoredValue(mailUri22, Data.IS_SUPER_PRIMARY, 0);
    }

    /**
     * Common function for the testNewPrimaryIn* functions. Its four configurations
     * are each called from its own test
     */
    public void testChangingPrimary(boolean inUpdate, boolean withSuperPrimary) {
        long rawContactId = createRawContact(new Account("a", "a"));
        Uri mailUri1 = insertEmail(rawContactId, "test1@domain1.com", true);

        if (withSuperPrimary) {
            final ContentValues values = new ContentValues();
            values.put(Data.IS_SUPER_PRIMARY, 1);
            mResolver.update(mailUri1, values, null, null);
        }

        assertStoredValue(mailUri1, Data.IS_PRIMARY, 1);
        assertStoredValue(mailUri1, Data.IS_SUPER_PRIMARY, withSuperPrimary ? 1 : 0);

        // Insert another item
        final Uri mailUri2;
        if (inUpdate) {
            mailUri2 = insertEmail(rawContactId, "test2@domain1.com");

            assertStoredValue(mailUri1, Data.IS_PRIMARY, 1);
            assertStoredValue(mailUri1, Data.IS_SUPER_PRIMARY, withSuperPrimary ? 1 : 0);
            assertStoredValue(mailUri2, Data.IS_PRIMARY, 0);
            assertStoredValue(mailUri2, Data.IS_SUPER_PRIMARY, 0);

            final ContentValues values = new ContentValues();
            values.put(Data.IS_PRIMARY, 1);
            mResolver.update(mailUri2, values, null, null);
        } else {
            // directly add as default
            mailUri2 = insertEmail(rawContactId, "test2@domain1.com", true);
        }

        // Ensure that primary has been unset on the first
        // If withSuperPrimary is set, also ensure that is has been moved to the new item
        assertStoredValue(mailUri1, Data.IS_PRIMARY, 0);
        assertStoredValue(mailUri1, Data.IS_SUPER_PRIMARY, 0);
        assertStoredValue(mailUri2, Data.IS_PRIMARY, 1);
        assertStoredValue(mailUri2, Data.IS_SUPER_PRIMARY, withSuperPrimary ? 1 : 0);
    }

    public void testNewPrimaryInInsert() {
        testChangingPrimary(false, false);
    }

    public void testNewPrimaryInInsertWithSuperPrimary() {
        testChangingPrimary(false, true);
    }

    public void testNewPrimaryInUpdate() {
        testChangingPrimary(true, false);
    }

    public void testNewPrimaryInUpdateWithSuperPrimary() {
        testChangingPrimary(true, true);
    }

    public void testContactCounts() {
        Uri uri = Contacts.CONTENT_URI.buildUpon()
                .appendQueryParameter(ContactCounts.ADDRESS_BOOK_INDEX_EXTRAS, "true").build();

        createRawContact();
        createRawContactWithName("James", "Sullivan");
        createRawContactWithName("The Abominable", "Snowman");
        createRawContactWithName("Mike", "Wazowski");
        createRawContactWithName("randall", "boggs");
        createRawContactWithName("Boo", null);
        createRawContactWithName("Mary", null);
        createRawContactWithName("Roz", null);

        Cursor cursor = mResolver.query(uri,
                new String[]{Contacts.DISPLAY_NAME},
                null, null, Contacts.SORT_KEY_PRIMARY + " COLLATE LOCALIZED");

        assertFirstLetterValues(cursor, null, "B", "J", "M", "R", "T");
        assertFirstLetterCounts(cursor,    1,   1,   1,   2,   2,   1);
        cursor.close();

        cursor = mResolver.query(uri,
                new String[]{Contacts.DISPLAY_NAME},
                null, null, Contacts.SORT_KEY_ALTERNATIVE + " COLLATE LOCALIZED DESC");

        assertFirstLetterValues(cursor, "W", "S", "R", "M", "B", null);
        assertFirstLetterCounts(cursor,   1,   2,   1,   1,   2,    1);
        cursor.close();
    }

    private void assertFirstLetterValues(Cursor cursor, String... expected) {
        String[] actual = cursor.getExtras()
                .getStringArray(ContactCounts.EXTRA_ADDRESS_BOOK_INDEX_TITLES);
        MoreAsserts.assertEquals(expected, actual);
    }

    private void assertFirstLetterCounts(Cursor cursor, int... expected) {
        int[] actual = cursor.getExtras()
                .getIntArray(ContactCounts.EXTRA_ADDRESS_BOOK_INDEX_COUNTS);
        MoreAsserts.assertEquals(expected, actual);
    }

    public void testReadBooleanQueryParameter() {
        assertBooleanUriParameter("foo:bar", "bool", true, true);
        assertBooleanUriParameter("foo:bar", "bool", false, false);
        assertBooleanUriParameter("foo:bar?bool=0", "bool", true, false);
        assertBooleanUriParameter("foo:bar?bool=1", "bool", false, true);
        assertBooleanUriParameter("foo:bar?bool=false", "bool", true, false);
        assertBooleanUriParameter("foo:bar?bool=true", "bool", false, true);
        assertBooleanUriParameter("foo:bar?bool=FaLsE", "bool", true, false);
        assertBooleanUriParameter("foo:bar?bool=false&some=some", "bool", true, false);
        assertBooleanUriParameter("foo:bar?bool=1&some=some", "bool", false, true);
        assertBooleanUriParameter("foo:bar?some=bool", "bool", true, true);
        assertBooleanUriParameter("foo:bar?bool", "bool", true, true);
    }

    private void assertBooleanUriParameter(String uriString, String parameter,
            boolean defaultValue, boolean expectedValue) {
        assertEquals(expectedValue, ContactsProvider2.readBooleanQueryParameter(
                Uri.parse(uriString), parameter, defaultValue));
    }

    public void testGetQueryParameter() {
        assertQueryParameter("foo:bar", "param", null);
        assertQueryParameter("foo:bar?param", "param", null);
        assertQueryParameter("foo:bar?param=", "param", "");
        assertQueryParameter("foo:bar?param=val", "param", "val");
        assertQueryParameter("foo:bar?param=val&some=some", "param", "val");
        assertQueryParameter("foo:bar?some=some&param=val", "param", "val");
        assertQueryParameter("foo:bar?some=some&param=val&else=else", "param", "val");
        assertQueryParameter("foo:bar?param=john%40doe.com", "param", "john@doe.com");
        assertQueryParameter("foo:bar?some_param=val", "param", null);
        assertQueryParameter("foo:bar?some_param=val1&param=val2", "param", "val2");
        assertQueryParameter("foo:bar?some_param=val1&param=", "param", "");
        assertQueryParameter("foo:bar?some_param=val1&param", "param", null);
        assertQueryParameter("foo:bar?some_param=val1&another_param=val2&param=val3",
                "param", "val3");
        assertQueryParameter("foo:bar?some_param=val1&param=val2&some_param=val3",
                "param", "val2");
        assertQueryParameter("foo:bar?param=val1&some_param=val2", "param", "val1");
        assertQueryParameter("foo:bar?p=val1&pp=val2", "p", "val1");
        assertQueryParameter("foo:bar?pp=val1&p=val2", "p", "val2");
        assertQueryParameter("foo:bar?ppp=val1&pp=val2&p=val3", "p", "val3");
        assertQueryParameter("foo:bar?ppp=val&", "p", null);
    }

    public void testMissingAccountTypeParameter() {
        // Try querying for RawContacts only using ACCOUNT_NAME
        final Uri queryUri = RawContacts.CONTENT_URI.buildUpon().appendQueryParameter(
                RawContacts.ACCOUNT_NAME, "lolwut").build();
        try {
            final Cursor cursor = mResolver.query(queryUri, null, null, null, null);
            fail("Able to query with incomplete account query parameters");
        } catch (IllegalArgumentException e) {
            // Expected behavior.
        }
    }

    public void testInsertInconsistentAccountType() {
        // Try inserting RawContact with inconsistent Accounts
        final Account red = new Account("red", "red");
        final Account blue = new Account("blue", "blue");

        final ContentValues values = new ContentValues();
        values.put(RawContacts.ACCOUNT_NAME, red.name);
        values.put(RawContacts.ACCOUNT_TYPE, red.type);

        final Uri insertUri = maybeAddAccountQueryParameters(RawContacts.CONTENT_URI, blue);
        try {
            mResolver.insert(insertUri, values);
            fail("Able to insert RawContact with inconsistent account details");
        } catch (IllegalArgumentException e) {
            // Expected behavior.
        }
    }

    public void testProviderStatusNoContactsNoAccounts() throws Exception {
        assertProviderStatus(ProviderStatus.STATUS_NO_ACCOUNTS_NO_CONTACTS);
    }

    public void testProviderStatusOnlyLocalContacts() throws Exception {
        long rawContactId = createRawContact();
        assertProviderStatus(ProviderStatus.STATUS_NORMAL);
        mResolver.delete(
                ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId), null, null);
        assertProviderStatus(ProviderStatus.STATUS_NO_ACCOUNTS_NO_CONTACTS);
    }

    public void testProviderStatusWithAccounts() throws Exception {
        assertProviderStatus(ProviderStatus.STATUS_NO_ACCOUNTS_NO_CONTACTS);
        mActor.setAccounts(new Account[]{ACCOUNT_1});
        ((ContactsProvider2)getProvider()).onAccountsUpdated(new Account[]{ACCOUNT_1});
        assertProviderStatus(ProviderStatus.STATUS_NORMAL);
        mActor.setAccounts(new Account[0]);
        ((ContactsProvider2)getProvider()).onAccountsUpdated(new Account[0]);
        assertProviderStatus(ProviderStatus.STATUS_NO_ACCOUNTS_NO_CONTACTS);
    }

    private void assertProviderStatus(int expectedProviderStatus) {
        Cursor cursor = mResolver.query(ProviderStatus.CONTENT_URI,
                new String[]{ProviderStatus.DATA1, ProviderStatus.STATUS}, null, null, null);
        assertTrue(cursor.moveToFirst());
        assertEquals(0, cursor.getLong(0));
        assertEquals(expectedProviderStatus, cursor.getInt(1));
        cursor.close();
    }

    public void testProperties() throws Exception {
        ContactsProvider2 provider = (ContactsProvider2)getProvider();
        ContactsDatabaseHelper helper = (ContactsDatabaseHelper)provider.getDatabaseHelper();
        assertNull(helper.getProperty("non-existent", null));
        assertEquals("default", helper.getProperty("non-existent", "default"));

        helper.setProperty("existent1", "string1");
        helper.setProperty("existent2", "string2");
        assertEquals("string1", helper.getProperty("existent1", "default"));
        assertEquals("string2", helper.getProperty("existent2", "default"));
        helper.setProperty("existent1", null);
        assertEquals("default", helper.getProperty("existent1", "default"));
    }

    private class VCardTestUriCreator {
        private String mLookup1;
        private String mLookup2;

        public VCardTestUriCreator(String lookup1, String lookup2) {
            super();
            mLookup1 = lookup1;
            mLookup2 = lookup2;
        }

        public Uri getUri1() {
            return Uri.withAppendedPath(Contacts.CONTENT_VCARD_URI, mLookup1);
        }

        public Uri getUri2() {
            return Uri.withAppendedPath(Contacts.CONTENT_VCARD_URI, mLookup2);
        }

        public Uri getCombinedUri() {
            return Uri.withAppendedPath(Contacts.CONTENT_MULTI_VCARD_URI,
                    Uri.encode(mLookup1 + ":" + mLookup2));
        }
    }

    private VCardTestUriCreator createVCardTestContacts() {
        final long rawContactId1 = createRawContact(mAccount, RawContacts.SOURCE_ID, "4:12");
        insertStructuredName(rawContactId1, "John", "Doe");

        final long rawContactId2 = createRawContact(mAccount, RawContacts.SOURCE_ID, "3:4%121");
        insertStructuredName(rawContactId2, "Jane", "Doh");

        final long contactId1 = queryContactId(rawContactId1);
        final long contactId2 = queryContactId(rawContactId2);
        final Uri contact1Uri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId1);
        final Uri contact2Uri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId2);
        final String lookup1 =
            Uri.encode(Contacts.getLookupUri(mResolver, contact1Uri).getPathSegments().get(2));
        final String lookup2 =
            Uri.encode(Contacts.getLookupUri(mResolver, contact2Uri).getPathSegments().get(2));
        return new VCardTestUriCreator(lookup1, lookup2);
    }

    public void testQueryMultiVCard() {
        // No need to create any contacts here, because the query for multiple vcards
        // does not go into the database at all
        Uri uri = Uri.withAppendedPath(Contacts.CONTENT_MULTI_VCARD_URI, Uri.encode("123:456"));
        Cursor cursor = mResolver.query(uri, null, null, null, null);
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        assertTrue(cursor.isNull(cursor.getColumnIndex(OpenableColumns.SIZE)));
        String filename = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));

        // The resulting name contains date and time. Ensure that before and after are correct
        assertTrue(filename.startsWith("vcards_"));
        assertTrue(filename.endsWith(".vcf"));
        cursor.close();
    }

    public void testQueryFileSingleVCard() {
        final VCardTestUriCreator contacts = createVCardTestContacts();

        {
            Cursor cursor = mResolver.query(contacts.getUri1(), null, null, null, null);
            assertEquals(1, cursor.getCount());
            assertTrue(cursor.moveToFirst());
            assertTrue(cursor.isNull(cursor.getColumnIndex(OpenableColumns.SIZE)));
            String filename = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            assertEquals("John Doe.vcf", filename);
            cursor.close();
        }

        {
            Cursor cursor = mResolver.query(contacts.getUri2(), null, null, null, null);
            assertEquals(1, cursor.getCount());
            assertTrue(cursor.moveToFirst());
            assertTrue(cursor.isNull(cursor.getColumnIndex(OpenableColumns.SIZE)));
            String filename = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            assertEquals("Jane Doh.vcf", filename);
            cursor.close();
        }
    }

    public void testQueryFileProfileVCard() {
        createBasicProfileContact(new ContentValues());
        Cursor cursor = mResolver.query(Profile.CONTENT_VCARD_URI, null, null, null, null);
        assertEquals(1, cursor.getCount());
        assertTrue(cursor.moveToFirst());
        assertTrue(cursor.isNull(cursor.getColumnIndex(OpenableColumns.SIZE)));
        String filename = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
        assertEquals("Mia Prophyl.vcf", filename);
        cursor.close();
    }

    public void testOpenAssetFileMultiVCard() throws IOException {
        final VCardTestUriCreator contacts = createVCardTestContacts();

        final AssetFileDescriptor descriptor =
            mResolver.openAssetFileDescriptor(contacts.getCombinedUri(), "r");
        final FileInputStream inputStream = descriptor.createInputStream();
        String data = readToEnd(inputStream);
        inputStream.close();
        descriptor.close();

        // Ensure that the resulting VCard has both contacts
        assertTrue(data.contains("N:Doe;John;;;"));
        assertTrue(data.contains("N:Doh;Jane;;;"));
    }

    public void testOpenAssetFileSingleVCard() throws IOException {
        final VCardTestUriCreator contacts = createVCardTestContacts();

        // Ensure that the right VCard is being created in each case
        {
            final AssetFileDescriptor descriptor =
                mResolver.openAssetFileDescriptor(contacts.getUri1(), "r");
            final FileInputStream inputStream = descriptor.createInputStream();
            final String data = readToEnd(inputStream);
            inputStream.close();
            descriptor.close();

            assertTrue(data.contains("N:Doe;John;;;"));
            assertFalse(data.contains("N:Doh;Jane;;;"));
        }

        {
            final AssetFileDescriptor descriptor =
                mResolver.openAssetFileDescriptor(contacts.getUri2(), "r");
            final FileInputStream inputStream = descriptor.createInputStream();
            final String data = readToEnd(inputStream);
            inputStream.close();
            descriptor.close();

            assertFalse(data.contains("N:Doe;John;;;"));
            assertTrue(data.contains("N:Doh;Jane;;;"));
        }
    }

    public void testAutoGroupMembership() {
        long g1 = createGroup(mAccount, "g1", "t1", 0, true /* autoAdd */, false /* favorite */);
        long g2 = createGroup(mAccount, "g2", "t2", 0, false /* autoAdd */, false /* favorite */);
        long g3 = createGroup(mAccountTwo, "g3", "t3", 0, true /* autoAdd */, false /* favorite */);
        long g4 = createGroup(mAccountTwo, "g4", "t4", 0, false /* autoAdd */, false/* favorite */);
        long r1 = createRawContact(mAccount);
        long r2 = createRawContact(mAccountTwo);
        long r3 = createRawContact(null);

        Cursor c = queryGroupMemberships(mAccount);
        try {
            assertTrue(c.moveToNext());
            assertEquals(g1, c.getLong(0));
            assertEquals(r1, c.getLong(1));
            assertFalse(c.moveToNext());
        } finally {
            c.close();
        }

        c = queryGroupMemberships(mAccountTwo);
        try {
            assertTrue(c.moveToNext());
            assertEquals(g3, c.getLong(0));
            assertEquals(r2, c.getLong(1));
            assertFalse(c.moveToNext());
        } finally {
            c.close();
        }
    }

    public void testNoAutoAddMembershipAfterGroupCreation() {
        long r1 = createRawContact(mAccount);
        long r2 = createRawContact(mAccount);
        long r3 = createRawContact(mAccount);
        long r4 = createRawContact(mAccountTwo);
        long r5 = createRawContact(mAccountTwo);
        long r6 = createRawContact(null);

        assertNoRowsAndClose(queryGroupMemberships(mAccount));
        assertNoRowsAndClose(queryGroupMemberships(mAccountTwo));

        long g1 = createGroup(mAccount, "g1", "t1", 0, true /* autoAdd */, false /* favorite */);
        long g2 = createGroup(mAccount, "g2", "t2", 0, false /* autoAdd */, false /* favorite */);
        long g3 = createGroup(mAccountTwo, "g3", "t3", 0, true /* autoAdd */, false/* favorite */);

        assertNoRowsAndClose(queryGroupMemberships(mAccount));
        assertNoRowsAndClose(queryGroupMemberships(mAccountTwo));
    }

    // create some starred and non-starred contacts, some associated with account, some not
    // favorites group created
    // the starred contacts should be added to group
    // favorites group removed
    // no change to starred status
    public void testFavoritesMembershipAfterGroupCreation() {
        long r1 = createRawContact(mAccount, RawContacts.STARRED, "1");
        long r2 = createRawContact(mAccount);
        long r3 = createRawContact(mAccount, RawContacts.STARRED, "1");
        long r4 = createRawContact(mAccountTwo, RawContacts.STARRED, "1");
        long r5 = createRawContact(mAccountTwo);
        long r6 = createRawContact(null, RawContacts.STARRED, "1");
        long r7 = createRawContact(null);

        assertNoRowsAndClose(queryGroupMemberships(mAccount));
        assertNoRowsAndClose(queryGroupMemberships(mAccountTwo));

        long g1 = createGroup(mAccount, "g1", "t1", 0, false /* autoAdd */, true /* favorite */);
        long g2 = createGroup(mAccount, "g2", "t2", 0, false /* autoAdd */, false /* favorite */);
        long g3 = createGroup(mAccountTwo, "g3", "t3", 0, false /* autoAdd */, false/* favorite */);

        assertTrue(queryRawContactIsStarred(r1));
        assertFalse(queryRawContactIsStarred(r2));
        assertTrue(queryRawContactIsStarred(r3));
        assertTrue(queryRawContactIsStarred(r4));
        assertFalse(queryRawContactIsStarred(r5));
        assertTrue(queryRawContactIsStarred(r6));
        assertFalse(queryRawContactIsStarred(r7));

        assertNoRowsAndClose(queryGroupMemberships(mAccountTwo));
        Cursor c = queryGroupMemberships(mAccount);
        try {
            assertTrue(c.moveToNext());
            assertEquals(g1, c.getLong(0));
            assertEquals(r1, c.getLong(1));
            assertTrue(c.moveToNext());
            assertEquals(g1, c.getLong(0));
            assertEquals(r3, c.getLong(1));
            assertFalse(c.moveToNext());
        } finally {
            c.close();
        }

        updateItem(RawContacts.CONTENT_URI, r6,
                RawContacts.ACCOUNT_NAME, mAccount.name,
                RawContacts.ACCOUNT_TYPE, mAccount.type);
        assertNoRowsAndClose(queryGroupMemberships(mAccountTwo));
        c = queryGroupMemberships(mAccount);
        try {
            assertTrue(c.moveToNext());
            assertEquals(g1, c.getLong(0));
            assertEquals(r1, c.getLong(1));
            assertTrue(c.moveToNext());
            assertEquals(g1, c.getLong(0));
            assertEquals(r3, c.getLong(1));
            assertTrue(c.moveToNext());
            assertEquals(g1, c.getLong(0));
            assertEquals(r6, c.getLong(1));
            assertFalse(c.moveToNext());
        } finally {
            c.close();
        }

        mResolver.delete(ContentUris.withAppendedId(Groups.CONTENT_URI, g1), null, null);

        assertNoRowsAndClose(queryGroupMemberships(mAccount));
        assertNoRowsAndClose(queryGroupMemberships(mAccountTwo));

        assertTrue(queryRawContactIsStarred(r1));
        assertFalse(queryRawContactIsStarred(r2));
        assertTrue(queryRawContactIsStarred(r3));
        assertTrue(queryRawContactIsStarred(r4));
        assertFalse(queryRawContactIsStarred(r5));
        assertTrue(queryRawContactIsStarred(r6));
        assertFalse(queryRawContactIsStarred(r7));
    }

    public void testFavoritesGroupMembershipChangeAfterStarChange() {
        long g1 = createGroup(mAccount, "g1", "t1", 0, false /* autoAdd */, true /* favorite */);
        long g2 = createGroup(mAccount, "g2", "t2", 0, false /* autoAdd */, false/* favorite */);
        long g4 = createGroup(mAccountTwo, "g4", "t4", 0, false /* autoAdd */, true /* favorite */);
        long g5 = createGroup(mAccountTwo, "g5", "t5", 0, false /* autoAdd */, false/* favorite */);
        long r1 = createRawContact(mAccount, RawContacts.STARRED, "1");
        long r2 = createRawContact(mAccount);
        long r3 = createRawContact(mAccountTwo);

        assertNoRowsAndClose(queryGroupMemberships(mAccountTwo));
        Cursor c = queryGroupMemberships(mAccount);
        try {
            assertTrue(c.moveToNext());
            assertEquals(g1, c.getLong(0));
            assertEquals(r1, c.getLong(1));
            assertFalse(c.moveToNext());
        } finally {
            c.close();
        }

        // remove the star from r1
        assertEquals(1, updateItem(RawContacts.CONTENT_URI, r1, RawContacts.STARRED, "0"));

        // Since no raw contacts are starred, there should be no group memberships.
        assertNoRowsAndClose(queryGroupMemberships(mAccount));
        assertNoRowsAndClose(queryGroupMemberships(mAccountTwo));

        // mark r1 as starred
        assertEquals(1, updateItem(RawContacts.CONTENT_URI, r1, RawContacts.STARRED, "1"));
        // Now that r1 is starred it should have a membership in the one groups from mAccount
        // that is marked as a favorite.
        // There should be no memberships in mAccountTwo since it has no starred raw contacts.
        assertNoRowsAndClose(queryGroupMemberships(mAccountTwo));
        c = queryGroupMemberships(mAccount);
        try {
            assertTrue(c.moveToNext());
            assertEquals(g1, c.getLong(0));
            assertEquals(r1, c.getLong(1));
            assertFalse(c.moveToNext());
        } finally {
            c.close();
        }

        // remove the star from r1
        assertEquals(1, updateItem(RawContacts.CONTENT_URI, r1, RawContacts.STARRED, "0"));
        // Since no raw contacts are starred, there should be no group memberships.
        assertNoRowsAndClose(queryGroupMemberships(mAccount));
        assertNoRowsAndClose(queryGroupMemberships(mAccountTwo));

        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, queryContactId(r1));
        assertNotNull(contactUri);

        // mark r1 as starred via its contact lookup uri
        assertEquals(1, updateItem(contactUri, Contacts.STARRED, "1"));
        // Now that r1 is starred it should have a membership in the one groups from mAccount
        // that is marked as a favorite.
        // There should be no memberships in mAccountTwo since it has no starred raw contacts.
        assertNoRowsAndClose(queryGroupMemberships(mAccountTwo));
        c = queryGroupMemberships(mAccount);
        try {
            assertTrue(c.moveToNext());
            assertEquals(g1, c.getLong(0));
            assertEquals(r1, c.getLong(1));
            assertFalse(c.moveToNext());
        } finally {
            c.close();
        }

        // remove the star from r1
        updateItem(contactUri, Contacts.STARRED, "0");
        // Since no raw contacts are starred, there should be no group memberships.
        assertNoRowsAndClose(queryGroupMemberships(mAccount));
        assertNoRowsAndClose(queryGroupMemberships(mAccountTwo));
    }

    public void testStarChangedAfterGroupMembershipChange() {
        long g1 = createGroup(mAccount, "g1", "t1", 0, false /* autoAdd */, true /* favorite */);
        long g2 = createGroup(mAccount, "g2", "t2", 0, false /* autoAdd */, false/* favorite */);
        long g4 = createGroup(mAccountTwo, "g4", "t4", 0, false /* autoAdd */, true /* favorite */);
        long g5 = createGroup(mAccountTwo, "g5", "t5", 0, false /* autoAdd */, false/* favorite */);
        long r1 = createRawContact(mAccount);
        long r2 = createRawContact(mAccount);
        long r3 = createRawContact(mAccountTwo);

        assertFalse(queryRawContactIsStarred(r1));
        assertFalse(queryRawContactIsStarred(r2));
        assertFalse(queryRawContactIsStarred(r3));

        Cursor c;

        // add r1 to one favorites group
        // r1's star should automatically be set
        // r1 should automatically be added to the other favorites group
        Uri urir1g1 = insertGroupMembership(r1, g1);
        assertTrue(queryRawContactIsStarred(r1));
        assertFalse(queryRawContactIsStarred(r2));
        assertFalse(queryRawContactIsStarred(r3));
        assertNoRowsAndClose(queryGroupMemberships(mAccountTwo));
        c = queryGroupMemberships(mAccount);
        try {
            assertTrue(c.moveToNext());
            assertEquals(g1, c.getLong(0));
            assertEquals(r1, c.getLong(1));
            assertFalse(c.moveToNext());
        } finally {
            c.close();
        }

        // remove r1 from one favorites group
        mResolver.delete(urir1g1, null, null);
        // r1's star should no longer be set
        assertFalse(queryRawContactIsStarred(r1));
        assertFalse(queryRawContactIsStarred(r2));
        assertFalse(queryRawContactIsStarred(r3));
        // there should be no membership rows
        assertNoRowsAndClose(queryGroupMemberships(mAccount));
        assertNoRowsAndClose(queryGroupMemberships(mAccountTwo));

        // add r3 to the one favorites group for that account
        // r3's star should automatically be set
        Uri urir3g4 = insertGroupMembership(r3, g4);
        assertFalse(queryRawContactIsStarred(r1));
        assertFalse(queryRawContactIsStarred(r2));
        assertTrue(queryRawContactIsStarred(r3));
        assertNoRowsAndClose(queryGroupMemberships(mAccount));
        c = queryGroupMemberships(mAccountTwo);
        try {
            assertTrue(c.moveToNext());
            assertEquals(g4, c.getLong(0));
            assertEquals(r3, c.getLong(1));
            assertFalse(c.moveToNext());
        } finally {
            c.close();
        }

        // remove r3 from the favorites group
        mResolver.delete(urir3g4, null, null);
        // r3's star should automatically be cleared
        assertFalse(queryRawContactIsStarred(r1));
        assertFalse(queryRawContactIsStarred(r2));
        assertFalse(queryRawContactIsStarred(r3));
        assertNoRowsAndClose(queryGroupMemberships(mAccount));
        assertNoRowsAndClose(queryGroupMemberships(mAccountTwo));
    }

    public void testReadOnlyRawContact() {
        long rawContactId = createRawContact();
        Uri rawContactUri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId);
        storeValue(rawContactUri, RawContacts.CUSTOM_RINGTONE, "first");
        storeValue(rawContactUri, RawContacts.RAW_CONTACT_IS_READ_ONLY, 1);

        storeValue(rawContactUri, RawContacts.CUSTOM_RINGTONE, "second");
        assertStoredValue(rawContactUri, RawContacts.CUSTOM_RINGTONE, "first");

        Uri syncAdapterUri = rawContactUri.buildUpon()
                .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "1")
                .build();
        storeValue(syncAdapterUri, RawContacts.CUSTOM_RINGTONE, "third");
        assertStoredValue(rawContactUri, RawContacts.CUSTOM_RINGTONE, "third");
    }

    public void testReadOnlyDataRow() {
        long rawContactId = createRawContact();
        Uri emailUri = insertEmail(rawContactId, "email");
        Uri phoneUri = insertPhoneNumber(rawContactId, "555-1111");

        storeValue(emailUri, Data.IS_READ_ONLY, "1");
        storeValue(emailUri, Email.ADDRESS, "changed");
        storeValue(phoneUri, Phone.NUMBER, "555-2222");
        assertStoredValue(emailUri, Email.ADDRESS, "email");
        assertStoredValue(phoneUri, Phone.NUMBER, "555-2222");

        Uri syncAdapterUri = emailUri.buildUpon()
                .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "1")
                .build();
        storeValue(syncAdapterUri, Email.ADDRESS, "changed");
        assertStoredValue(emailUri, Email.ADDRESS, "changed");
    }

    public void testContactWithReadOnlyRawContact() {
        long rawContactId1 = createRawContact();
        Uri rawContactUri1 = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId1);
        storeValue(rawContactUri1, RawContacts.CUSTOM_RINGTONE, "first");

        long rawContactId2 = createRawContact();
        Uri rawContactUri2 = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId2);
        storeValue(rawContactUri2, RawContacts.CUSTOM_RINGTONE, "second");
        storeValue(rawContactUri2, RawContacts.RAW_CONTACT_IS_READ_ONLY, 1);

        setAggregationException(AggregationExceptions.TYPE_KEEP_TOGETHER,
                rawContactId1, rawContactId2);

        long contactId = queryContactId(rawContactId1);

        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
        storeValue(contactUri, Contacts.CUSTOM_RINGTONE, "rt");
        assertStoredValue(contactUri, Contacts.CUSTOM_RINGTONE, "rt");
        assertStoredValue(rawContactUri1, RawContacts.CUSTOM_RINGTONE, "rt");
        assertStoredValue(rawContactUri2, RawContacts.CUSTOM_RINGTONE, "second");
    }

    public void testNameParsingQuery() {
        Uri uri = ContactsContract.AUTHORITY_URI.buildUpon().appendPath("complete_name")
                .appendQueryParameter(StructuredName.DISPLAY_NAME, "Mr. John Q. Doe Jr.").build();
        Cursor cursor = mResolver.query(uri, null, null, null, null);
        ContentValues values = new ContentValues();
        values.put(StructuredName.DISPLAY_NAME, "Mr. John Q. Doe Jr.");
        values.put(StructuredName.PREFIX, "Mr.");
        values.put(StructuredName.GIVEN_NAME, "John");
        values.put(StructuredName.MIDDLE_NAME, "Q.");
        values.put(StructuredName.FAMILY_NAME, "Doe");
        values.put(StructuredName.SUFFIX, "Jr.");
        values.put(StructuredName.FULL_NAME_STYLE, FullNameStyle.WESTERN);
        assertTrue(cursor.moveToFirst());
        assertCursorValues(cursor, values);
        cursor.close();
    }

    public void testNameConcatenationQuery() {
        Uri uri = ContactsContract.AUTHORITY_URI.buildUpon().appendPath("complete_name")
                .appendQueryParameter(StructuredName.PREFIX, "Mr")
                .appendQueryParameter(StructuredName.GIVEN_NAME, "John")
                .appendQueryParameter(StructuredName.MIDDLE_NAME, "Q.")
                .appendQueryParameter(StructuredName.FAMILY_NAME, "Doe")
                .appendQueryParameter(StructuredName.SUFFIX, "Jr.")
                .build();
        Cursor cursor = mResolver.query(uri, null, null, null, null);
        ContentValues values = new ContentValues();
        values.put(StructuredName.DISPLAY_NAME, "Mr John Q. Doe, Jr.");
        values.put(StructuredName.PREFIX, "Mr");
        values.put(StructuredName.GIVEN_NAME, "John");
        values.put(StructuredName.MIDDLE_NAME, "Q.");
        values.put(StructuredName.FAMILY_NAME, "Doe");
        values.put(StructuredName.SUFFIX, "Jr.");
        values.put(StructuredName.FULL_NAME_STYLE, FullNameStyle.WESTERN);
        assertTrue(cursor.moveToFirst());
        assertCursorValues(cursor, values);
        cursor.close();
    }

    private Cursor queryGroupMemberships(Account account) {
        Cursor c = mResolver.query(maybeAddAccountQueryParameters(Data.CONTENT_URI, account),
                new String[]{GroupMembership.GROUP_ROW_ID, GroupMembership.RAW_CONTACT_ID},
                Data.MIMETYPE + "=?", new String[]{GroupMembership.CONTENT_ITEM_TYPE},
                GroupMembership.GROUP_SOURCE_ID);
        return c;
    }

    private String readToEnd(FileInputStream inputStream) {
        try {
            System.out.println("DECLARED INPUT STREAM LENGTH: " + inputStream.available());
            int ch;
            StringBuilder stringBuilder = new StringBuilder();
            int index = 0;
            while (true) {
                ch = inputStream.read();
                System.out.println("READ CHARACTER: " + index + " " + ch);
                if (ch == -1) {
                    break;
                }
                stringBuilder.append((char)ch);
                index++;
            }
            return stringBuilder.toString();
        } catch (IOException e) {
            return null;
        }
    }

    private void assertQueryParameter(String uriString, String parameter, String expectedValue) {
        assertEquals(expectedValue, ContactsProvider2.getQueryParameter(
                Uri.parse(uriString), parameter));
    }

    private long createContact(ContentValues values, String firstName, String givenName,
            String phoneNumber, String email, int presenceStatus, int timesContacted, int starred,
            long groupId, int chatMode) {
        return createContact(values, firstName, givenName, phoneNumber, email, presenceStatus,
                timesContacted, starred, groupId, chatMode, false);
    }

    private long createContact(ContentValues values, String firstName, String givenName,
            String phoneNumber, String email, int presenceStatus, int timesContacted, int starred,
            long groupId, int chatMode, boolean isUserProfile) {
        return queryContactId(createRawContact(values, firstName, givenName, phoneNumber, email,
                presenceStatus, timesContacted, starred, groupId, chatMode, isUserProfile));
    }

    private long createRawContact(ContentValues values, String firstName, String givenName,
            String phoneNumber, String email, int presenceStatus, int timesContacted, int starred,
            long groupId, int chatMode) {
        long rawContactId = createRawContact(values, phoneNumber, email, presenceStatus,
                timesContacted, starred, groupId, chatMode);
        insertStructuredName(rawContactId, firstName, givenName);
        return rawContactId;
    }

    private long createRawContact(ContentValues values, String firstName, String givenName,
            String phoneNumber, String email, int presenceStatus, int timesContacted, int starred,
            long groupId, int chatMode, boolean isUserProfile) {
        long rawContactId = createRawContact(values, phoneNumber, email, presenceStatus,
                timesContacted, starred, groupId, chatMode, isUserProfile);
        insertStructuredName(rawContactId, firstName, givenName);
        return rawContactId;
    }

    private long createRawContact(ContentValues values, String phoneNumber, String email,
            int presenceStatus, int timesContacted, int starred, long groupId, int chatMode) {
        return createRawContact(values, phoneNumber, email, presenceStatus, timesContacted, starred,
                groupId, chatMode, false);
    }

    private long createRawContact(ContentValues values, String phoneNumber, String email,
            int presenceStatus, int timesContacted, int starred, long groupId, int chatMode,
            boolean isUserProfile) {
        values.put(RawContacts.STARRED, starred);
        values.put(RawContacts.SEND_TO_VOICEMAIL, 1);
        values.put(RawContacts.CUSTOM_RINGTONE, "beethoven5");
        values.put(RawContacts.TIMES_CONTACTED, timesContacted);

        Uri insertionUri = isUserProfile
                ? Profile.CONTENT_RAW_CONTACTS_URI
                : RawContacts.CONTENT_URI;
        Uri rawContactUri = mResolver.insert(insertionUri, values);
        long rawContactId = ContentUris.parseId(rawContactUri);
        Uri photoUri = insertPhoto(rawContactId);
        long photoId = ContentUris.parseId(photoUri);
        values.put(Contacts.PHOTO_ID, photoId);
        if (!TextUtils.isEmpty(phoneNumber)) {
            insertPhoneNumber(rawContactId, phoneNumber);
        }
        if (!TextUtils.isEmpty(email)) {
            insertEmail(rawContactId, email);
        }

        insertStatusUpdate(Im.PROTOCOL_GOOGLE_TALK, null, email, presenceStatus, "hacking",
                chatMode, isUserProfile);

        if (groupId != 0) {
            insertGroupMembership(rawContactId, groupId);
        }

        return rawContactId;
    }

    /**
     * Creates a raw contact with pre-set values under the user's profile.
     * @param profileValues Values to be used to create the entry (common values will be
     *     automatically populated in createRawContact()).
     * @return the raw contact ID that was created.
     */
    private long createBasicProfileContact(ContentValues profileValues) {
        long profileRawContactId = createRawContact(profileValues, "Mia", "Prophyl",
                "18005554411", "mia.prophyl@acme.com", StatusUpdates.INVISIBLE, 4, 1, 0,
                StatusUpdates.CAPABILITY_HAS_CAMERA, true);
        profileValues.put(Contacts.DISPLAY_NAME, "Mia Prophyl");
        return profileRawContactId;
    }

    /**
     * Creates a raw contact with pre-set values that is not under the user's profile.
     * @param nonProfileValues Values to be used to create the entry (common values will be
     *     automatically populated in createRawContact()).
     * @return the raw contact ID that was created.
     */
    private long createBasicNonProfileContact(ContentValues nonProfileValues) {
        long nonProfileRawContactId = createRawContact(nonProfileValues, "John", "Doe",
                "18004664411", "goog411@acme.com", StatusUpdates.INVISIBLE, 4, 1, 0,
                StatusUpdates.CAPABILITY_HAS_CAMERA, false);
        nonProfileValues.put(Contacts.DISPLAY_NAME, "John Doe");
        return nonProfileRawContactId;
    }

    private void putDataValues(ContentValues values, long rawContactId) {
        values.put(Data.RAW_CONTACT_ID, rawContactId);
        values.put(Data.MIMETYPE, "testmimetype");
        values.put(Data.RES_PACKAGE, "oldpackage");
        values.put(Data.IS_PRIMARY, 1);
        values.put(Data.IS_SUPER_PRIMARY, 1);
        values.put(Data.DATA1, "one");
        values.put(Data.DATA2, "two");
        values.put(Data.DATA3, "three");
        values.put(Data.DATA4, "four");
        values.put(Data.DATA5, "five");
        values.put(Data.DATA6, "six");
        values.put(Data.DATA7, "seven");
        values.put(Data.DATA8, "eight");
        values.put(Data.DATA9, "nine");
        values.put(Data.DATA10, "ten");
        values.put(Data.DATA11, "eleven");
        values.put(Data.DATA12, "twelve");
        values.put(Data.DATA13, "thirteen");
        values.put(Data.DATA14, "fourteen");
        values.put(Data.DATA15, "fifteen");
        values.put(Data.SYNC1, "sync1");
        values.put(Data.SYNC2, "sync2");
        values.put(Data.SYNC3, "sync3");
        values.put(Data.SYNC4, "sync4");
    }

    /**
     * @param data1 email address or phone number
     * @param usageType One of {@link DataUsageFeedback#USAGE_TYPE}
     * @param values ContentValues for this feedback. Useful for incrementing
     * {Contacts#TIMES_CONTACTED} in the ContentValue. Can be null.
     */
    private void sendFeedback(String data1, String usageType, ContentValues values) {
        final long dataId = getStoredLongValue(Data.CONTENT_URI,
                Data.DATA1 + "=?", new String[] { data1 }, Data._ID);
        final Uri feedbackUri = DataUsageFeedback.FEEDBACK_URI.buildUpon()
                .appendPath(String.valueOf(dataId))
                .appendQueryParameter(DataUsageFeedback.USAGE_TYPE, usageType)
                .build();
        assertNotSame(0, mResolver.update(feedbackUri, new ContentValues(), null, null));
        if (values != null && values.containsKey(Contacts.TIMES_CONTACTED)) {
            values.put(Contacts.TIMES_CONTACTED, values.getAsInteger(Contacts.TIMES_CONTACTED) + 1);
        }
    }
}
