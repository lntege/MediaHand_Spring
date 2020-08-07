create sequence hibernate_sequence start with 1 increment by 1

    create table DirectoryEntry (
       id integer not null,
        path varchar(255),
        primary key (id)
    )

    create table MediaEntry (
       id integer not null,
        added date,
        audioTrack varchar(255),
        currentEpisode integer not null,
        episodeLength integer not null,
        episodeNumber integer not null,
        mediaType varchar(255),
        path varchar(255),
        rating integer not null,
        subtitleTrack varchar(255),
        title varchar(255),
        volume integer not null,
        watchState varchar(255),
        watchedCount integer not null,
        watchedDate date,
        basePath_id integer,
        primary key (id)
    )

    create table SettingsEntry (
       id integer not null,
        autoContinue boolean not null,
        profile varchar(255),
        showAll boolean not null,
        watchState varchar(255),
        windowHeight integer not null,
        windowWidth integer not null,
        primary key (id)
    )

    alter table DirectoryEntry 
       add constraint UK_bt0b3p8lm8yi06i7ux7ubq4o4 unique (path)

    alter table MediaEntry 
       add constraint UK_gxy37bxxmxgnvgbkrdo5nv6gk unique (title)

    alter table SettingsEntry 
       add constraint UK_6dwq37vrquhxaamp63e5dw81h unique (profile)

    alter table MediaEntry 
       add constraint FK1uuo2mxrkaxfc2k28jtidcu19 
       foreign key (basePath_id) 
       references DirectoryEntry
