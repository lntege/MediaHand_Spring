create sequence hibernate_sequence start with 1 increment by 1

    create table DirectoryEntry (
        id integer not null,
        path varchar(255),
        primary key (id)
    )

    create table MediaEntry (
        id integer not null,
        title varchar(255),
        episodeNumber integer not null,
        mediaType varchar(255),
        watchState varchar(255),
        rating integer not null,
        path varchar(255),
        currentEpisode integer not null,
        added date,
        episodeLength integer not null,
        watchedDate date,
        watchedCount integer not null,
        basePath_id integer,
        volume integer not null,
        audioTrack varchar(255),
        subtitleTrack varchar(255),
        primary key (id)
    )

    create table SettingsEntry (
        id integer not null,
        profile varchar(255),
        windowWidth integer not null,
        windowHeight integer not null,
        autoContinue boolean not null,
        showAll boolean not null,
        watchState varchar(255),
        primary key (id)
    )
