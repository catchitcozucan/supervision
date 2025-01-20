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

yum install -y  postgresql-contrib
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
