package com.cooliris.picasa;

@Entry.Table("users")
public final class UserEntry extends Entry {
    public static final EntrySchema SCHEMA = new EntrySchema(UserEntry.class);

    @Column("account")
    public String account;

    @Column("albums_etag")
    public String albumsEtag;
}
