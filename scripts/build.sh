#!/usr/bin/env bash

(
set -e
basedir="$(cd "$1" && pwd -P)"
workdir="$basedir/work"
paperbasedir="$basedir/work/Paper"
paperworkdir="$basedir/work/Paper/work"

if [ "$2" == "--setup" ] || [ "$3" == "--setup" ] || [ "$4" == "--setup" ]; then
	echo "[Akarin] Setup Paper.."
	(
		cd "$paperbasedir"
		./paper patch
	)
fi

echo "[KianaMC] Ready to build"
(
	cd "$paperbasedir"
	echo "[KianaMC] Touch sources.."
	
	cd "$paperbasedir"
	if [ "$2" == "--fast" ] || [ "$3" == "--fast" ] || [ "$4" == "--fast" ]; then
		echo "[KianaMC] Test and repatch has been skipped"
		\cp -rf "$basedir/api/src/main" "$paperbasedir/Paper-API/src/"
		\cp -rf "$basedir/api/pom.xml" "$paperbasedir/Paper-API/"
		\cp -rf "$basedir/src" "$paperbasedir/Paper-Server/"
		\cp -rf "$basedir/pom.xml" "$paperbasedir/Paper-Server/"
		mvn clean install -DskipTests -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true
	else
		rm -rf Paper-API/src
		rm -rf Paper-Server/src
		./paper patch
		\cp -rf "$basedir/api/src/main" "$paperbasedir/Paper-API/src/"
		\cp -rf "$basedir/api/pom.xml" "$paperbasedir/Paper-API/"
		\cp -rf "$basedir/src" "$paperbasedir/Paper-Server/"
		\cp -rf "$basedir/pom.xml" "$paperbasedir/Paper-Server/"
		mvn clean install -DskipTests -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true
	fi
	
	minecraftversion=$(cat "$paperworkdir/BuildData/info.json" | grep minecraftVersion | cut -d '"' -f 4)
	rawjar="$paperbasedir/Paper-Server/target/akarin-$minecraftversion.jar"
	\cp -rf "$rawjar" "$basedir/KianaMC-$minecraftversion.jar"
	rawapi="$paperbasedir/Paper-API/target/akarin-api-1.13.2-R0.1-SNAPSHOT.jar"
	\cp -rf "$rawapi" "$basedir/akarin-api-1.13.2-R0.1-SNAPSHOT.jar"
	
	echo ""
	echo "[KianaMC] Build successful"
	echo "[KianaMC] Migrated the final jar to $basedir/"
)

)
