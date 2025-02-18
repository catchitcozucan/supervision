--
--    Original work by Ola Aronsson 2020
--    Courtesy of nollettnoll AB &copy; 2012 - 2020
--
--    Licensed under the Creative Commons Attribution 4.0 International (the "License")
--    you may not use this file except in compliance with the License. You may obtain
--    a copy of the License at
--
--                https://creativecommons.org/licenses/by/4.0/
--
--    The software is provided “as is”, without warranty of any kind, express or
--    implied, including but not limited to the warranties of merchantability,
--    fitness for a particular purpose and noninfringement. In no event shall the
--    authors or copyright holders be liable for any claim, damages or other liability,
--    whether in an action of contract, tort or otherwise, arising from, out of or
--    in connection with the software or the use or other dealings in the software.
--

--
--    Of course you can change user and schema or whatever you like
--    - this is just an example database-setup for postgresql but
--    it does, of course, match the springboot app's current
--    database and flyway config etc.
--

Example installation for Linux :

https://docs.fedoraproject.org/en-US/quick-docs/postgresql

For windows it will mean clicking through loads of guis as per
usual.

Now you need to create the catchit database, the supervision schema
and the catchit user to access. This can be done cmdline in Linux
like (in windows you would do something similar but then probably
using some kind of SQL workbench, personally I use dbeaver which
is free, multi-platform and really nice):

sudo -u postgres psql <<!
CREATE DATABASE catchit;
!

sudo -u postgres psql catchit <<!
CREATE role catchit login password 'catchit';
CREATE SCHEMA supervision;
GRANT USAGE ON SCHEMA supervision TO catchit;
GRANT ALL ON SCHEMA supervision TO catchit;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA supervision TO catchit;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA supervision TO catchit;
GRANT CREATE ON SCHEMA supervision TO catchit;
GRANT ALL ON ALL TABLES IN SCHEMA supervision TO catchit;
!

If you have connection issues complaining about the access even after
above is executed and you want to make sure everyone in your network can
connect to your database, then try (and 192.168.0.0/16 is in this example
the internel network I want to allow access):

root@fedora:~# echo "listen_addresses='*' >>  /var/lib/pgsql/data/postgresql.conf
root@fedora:~# vi /var/lib/pgsql/data/pg_hba.conf

add the section

host    all             all              127.0.0.1/32            md5
host    all             all              192.168.0.0/16          md5
host    all             all              ::/0                    md5

lastly.

Now restart postgresql:

root@fedora:~# systemctl restart postgresql

Access should be fine, the catchit database online with the supervision schema
and the catchit user setup. This means that the server is ready for the springboot
driven flyway table definitions to run - you are now ready to startup the
application!
