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
