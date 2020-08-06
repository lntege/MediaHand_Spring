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
        available boolean not null,
        currentEpisode integer not null,
        episodeLength integer not null,
        episodeNumber integer not null,
        mediaType varchar(255),
        path varchar(255),
        rating integer not null,
        subtitleTrack varchar(255),
        title varchar(255),
        volume integer not null,
        watchState integer,
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
        watchState integer,
        windowHeight integer not null,
        windowWidth integer not null,
        primary key (id)
    )

    alter table MediaEntry 
       add constraint FK1uuo2mxrkaxfc2k28jtidcu19 
       foreign key (basePath_id) 
       references DirectoryEntry
