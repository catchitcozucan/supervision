pushd src/main/react/catchitsupervision-ui && echo -n "cleaning react/npm.."
rm -rf node node_modules package-lock.json && npm cache clean --force
echo "done"
popd
mvn clean install package -U && java -jar target/catchitsupervision.jar

