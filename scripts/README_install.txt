For local development you need

- a JDK      : at least 21
- node js    : grab the latest one you can find
- postgresql : grab the latest one you can find
- maven      : grab the latest one you can find
- dbeaver or some other SQL workbench thing might come in handy

Set up a decent shell, I use cygwin for windows but just whatever you
usually do in order to perform maven builds as conveniently and
swiftly as possible.

The clone is already cloned as you can read this note so you
obviously have the code already.

1. Setup postgres - check the create_db.sql
2. Build the thing. A good first build would be like

>  mvn clean install -DskipTests -Pgen-api -DskipUI

in order to generate the typesript objects from the java api-package.
Then just

> mvn clean install

or however you do it usually.

3. In your IDE, I use Intellij, startup the springboot application. The very
first time it might not start but stop with the complaint

***************************
APPLICATION FAILED TO START
***************************

Description:

Field buildProperties in com.github.catchitcozucan.supervision.controllers.BuildInfoController
required a bean of type 'org.springframework.boot.info.BuildProperties' that could not be found.

Then you just do

> mvn package

and the build props will now be available and it will start fine.

4. In your IDE set up a launcher for the react gui. I use a launcher of type
'npm', target start, point out the package.json in the code's gui and tell
intellij to use the node JS I have downloaded. Now run it. You should be
presented with the demo view as you have not added any sources yet.

5. Click 'Login'. Only admin can modify or even fetch the system's source
configurations. The very first time you log in your admin password will be
checked for validity. Default it crap (or it is 'admin') and you will be
asked to provide a proper password to use. This password will be saved
and then you will be automagically logged in. The sources are now available
to you as you have an admin session running in your client.

6. Click 'Sources' and the either edit one of the demo once by pointing it to
an actual source. Test it with the 'Test' button so that you know that it
works. Press 'Save' - you are now in a live view with your first actual
process supervision running. Click 'Autoplay' in order to get automatic updates
every fifth second or so.

Yup, that's pretty much it.

/Ola.
