
## Vultr

* Fedora Fedora 28 x64
* 1 vCPU
* 1 GB RAM
* 25 GB SSD

## Build the Go image

```
rm -rf docker
mkdir -p docker

cat << EOF > docker/Dockerfile
FROM fedora:28

RUN dnf -y install golang

CMD ["version"]

ENTRYPOINT ["go"]
EOF

docker rmi -f nessusio/golang; docker build -t nessusio/golang docker/
docker run --rm nessusio/golang

docker push nessusio/golang
```

## Build the IPFS image

[Run IPFS latest on a VPS](https://ipfs.io/blog/22-run-ipfs-on-a-vps/);

```
export IPFS_VERSION=0.4.16
export IPFS_PLATFORM=linux-386
wget --no-check-certificate https://dist.ipfs.io/go-ipfs/v$IPFS_VERSION/go-ipfs_v"$IPFS_VERSION"_"$IPFS_PLATFORM".tar.gz

rm -rf docker
mkdir -p docker

tar xzf go-ipfs_*.tar.gz -C docker/

cat << EOF > docker/run.sh
#!/bin/bash

IPFS_CONFIG="/root/.ipfs/config"
if [ ! -f IPFS_CONFIG ]; then

    ipfs init
    
    ipfs config Addresses.API "/ip4/0.0.0.0/tcp/5001"
    ipfs config Addresses.Gateway "/ip4/0.0.0.0/tcp/8080"
    ipfs config --json Addresses.Swarm '["/ip4/0.0.0.0/tcp/4001"]'
fi

# Start the IPFS daemon
ipfs daemon
EOF

cat << EOF > docker/Dockerfile
FROM nessusio/golang

COPY go-ipfs/ipfs /usr/local/bin/

COPY run.sh /root/run.sh
RUN chmod +x /root/run.sh

ENTRYPOINT ["/root/run.sh"]
EOF

docker rmi -f nessusio/ipfs
docker build -t nessusio/ipfs docker/
docker push nessusio/ipfs

docker tag nessusio/ipfs nessusio/ipfs:$IPFS_VERSION
docker push nessusio/ipfs:$IPFS_VERSION
```

Run the IPFS image 

```
export NAME=ipfs-01

docker rm -f $NAME
docker run --detach \
    -p 4001:4001 \
    -p 8080:8080 \
    --expose 5001 \
    --memory=200m --memory-swap=2g \
    --name $NAME \
    nessusio/ipfs

docker logs $NAME
```

## Build the IPFS Java image

```
rm -rf docker
mkdir -p docker

cat << EOF > docker/Dockerfile
FROM nessusio/ipfs

# Install dependencies
RUN dnf -y install java

CMD ["-version"]
ENTRYPOINT ["java"]
EOF

docker rmi -f nessusio/ipfsj
docker build -t nessusio/ipfsj docker/
docker push nessusio/ipfsj

docker tag nessusio/ipfsj nessusio/ipfsj:$IPFS_VERSION
docker push nessusio/ipfsj:$IPFS_VERSION
```

## Build the AEG Image

Extract bootstrap data from a container

```
export NVERSION=2.0
export MNNAME=aeg-mn-01
export BLOCKCOUNT=`docker exec $MNNAME aegeus-cli getblockcount`
export MNARCHIVE=aegeusd-$NVERSION-$BLOCKCOUNT

docker stop $MNNAME
docker cp $MNNAME:/var/lib/aegeusd aegeusd
docker start $MNNAME

cd aegeusd
rm -rf *.old *.dat *.log *.conf backups database
cd ..

tar czf $MNARCHIVE.tgz aegeusd
rm -rf aegeusd
```

Build AEG image with bootstrap

```
rm -rf docker
mkdir docker

export NVERSION=2.0
export MNARCHIVE=aegeusd-2.0-230763
tar xzf $MNARCHIVE.tgz -C docker/

cat << EOF > docker/aegeus-server.conf
rpcuser=aeg
rpcpassword=aegpass
rpcport=51473
rpcallowip=127.0.0.1
rpcallowip=172.17.0.1/24
staking=1
listen=1
server=1
EOF

cat << EOF > docker/aegeus-client.conf
rpcuser=aeg
rpcpassword=aegpass
rpcport=51473
EOF

cat << EOF > docker/Dockerfile
# Based in Ubuntu 16.04
FROM ubuntu:16.04

# Install the binaries
COPY aegeusd/aegeusd /usr/bin/
COPY aegeusd/aegeus-cli /usr/bin/

# Prime blockchain data
COPY aegeusd /var/lib/aegeusd

# Install the config files
COPY aegeus-server.conf /etc/aegeus/aegeus.conf
COPY aegeus-client.conf /root/.aegeus/aegeus.conf

# System update/upgrade
RUN apt -y update
RUN apt -y upgrade
RUN apt -y install python3 software-properties-common
RUN apt-add-repository -y ppa:bitcoin/bitcoin
RUN apt -y update
RUN apt -y upgrade
RUN apt -y install make software-properties-common build-essential libtool autoconf libssl-dev libboost-dev libboost-chrono-dev libboost-filesystem-dev libboost-program-options-dev libboost-system-dev libboost-test-dev libboost-thread-dev automake git wget curl libdb4.8-dev bsdmainutils libdb4.8++-dev libminiupnpc-dev libgmp3-dev ufw pkg-config libevent-dev libdb5.3++ unzip libzmq5

# Expose the API port
EXPOSE 51473

# Set some default env vars
ENV RPCUSER=aeg
ENV RPCPASS=aegpass

# Provide (dummy) default params 
CMD ["-externalip=1.2.3.4:29328", "-masternodeprivkey=65u8854****"]

# Use the daemon as entry point 
ENTRYPOINT ["aegeusd", "-datadir=/var/lib/aegeusd", "-conf=/etc/aegeus/aegeus.conf"]
EOF

docker rmi -f nessusio/aegeus
docker build -t nessusio/aegeus docker/
docker push nessusio/aegeus

docker tag nessusio/aegeus nessusio/aegeus:$NVERSION
docker push nessusio/aegeus:$NVERSION
```

Run the AEG image

```
export MNNAME=aeg-01
export MNEXTIP=167.99.32.83
export MNPORT=29328

docker rm -f $MNNAME
docker run --detach \
    -p $MNPORT:$MNPORT \
    --memory=200m --memory-swap=2g \
    --env RPCUSER=aeg \
    --env RPCPASS=aegpass \
    --name $MNNAME \
    nessusio/aegeus -externalip=$MNEXTIP:$MNPORT

watch docker exec $MNNAME aegeus-cli getinfo

docker exec $MNNAME aegeus-cli addnode 94.16.118.207:29328 onetry
docker exec $MNNAME tail -f /var/lib/aegeusd/debug.log
```

## Build the AEG JAXRS image

```
rm -rf docker
mkdir -p docker

export NVERSION=1.0.0.Alpha3

tar xzf aegeus-dist-$NVERSION-deps.tgz -C docker
tar xzf aegeus-dist-$NVERSION-proj.tgz -C docker

cat << EOF > docker/Dockerfile
FROM nessusio/ipfsj

# Install the binaries
COPY aegeus-dist-$NVERSION aegeus-jaxrs

# Make the entrypoint executable
RUN ln -s /aegeus-jaxrs/bin/run-aegeus-jaxrs.sh /usr/local/bin/aegeus-jaxrs

CMD ["start"]
ENTRYPOINT ["aegeus-jaxrs"]
EOF

docker rmi -f nessusio/aegeus-jaxrs
docker build -t nessusio/aegeus-jaxrs docker/
docker push nessusio/aegeus-jaxrs

docker tag nessusio/aegeus-jaxrs nessusio/aegeus-jaxrs:$NVERSION
docker push nessusio/aegeus-jaxrs:$NVERSION
```

Run the AEG JAXRS image

```
export NAME=jaxrs-01
export MNNAME=aeg-01
export IPFSNAME=ipfs-01
export JAXRSPORT=8081

docker rm -f $NAME
docker run --detach \
    --link $MNNAME:aeg \
    --link $IPFSNAME:ipfs \
    -p $JAXRSPORT:$JAXRSPORT \
    --memory=200m --memory-swap=2g \
    --name $NAME \
    nessusio/aegeus-jaxrs
    
watch docker logs $NAME

docker exec $NAME aegeus-jaxrs --help
docker exec -it $NAME tail -f -n 100 debug.log

docker exec $NAME ipfs --api=/ip4/172.17.0.2/tcp/5001 config show
```

## Build the AEG WebUI image

```
rm -rf docker
mkdir -p docker

export NVERSION=1.0.0.Alpha3

tar xzf aegeus-dist-$NVERSION-deps.tgz -C docker
tar xzf aegeus-dist-$NVERSION-proj.tgz -C docker

cat << EOF > docker/Dockerfile
FROM nessusio/ipfsj

# Install the binaries
COPY aegeus-dist-$NVERSION aegeus-webui

ENTRYPOINT ["aegeus-webui/bin/run-aegeus-webui.sh"]
EOF

docker rmi -f nessusio/aegeus-webui
docker build -t nessusio/aegeus-webui docker/
docker push nessusio/aegeus-webui

docker tag nessusio/aegeus-webui nessusio/aegeus-webui:$NVERSION
docker push nessusio/aegeus-webui:$NVERSION
```

Run the AEG WebUI

```
export LABEL=Bob
export NAME=webui-01
export MNNAME=aeg-01
export IPFSNAME=ipfs-01
export JAXRSNAME=jaxrs-01
export MNEXTIP=167.99.32.83
export WEBPORT=5002

cat << EOF > aeg-webui-env
AEG_WEBUI_EXTIP=$MNEXTIP
AEG_WEBUI_HOST=0.0.0.0
AEG_WEBUI_PORT=$WEBPORT
AEG_WEBUI_LABEL=$LABEL
EOF

docker rm -f $NAME
docker run --detach \
    -p $WEBPORT:$WEBPORT \
    --link $MNNAME:aeg \
    --link $IPFSNAME:ipfs \
    --link $JAXRSNAME:jaxrs \
    --env-file=aeg-webui-env \
    --memory=200m --memory-swap=2g \
    --name $NAME \
    nessusio/aegeus-webui
    
watch docker logs $NAME

docker exec -it $NAME tail -f -n 100 debug.log

docker exec $NAME ipfs config show
docker exec -it $NAME bash

docker exec $NAME ipfs --api=/ip4/172.17.0.3/tcp/5001 config show
 
docker rm -f $(docker ps -qa)
```

## Test Zone

```
rm -rf docker
mkdir -p docker

cat << EOF > docker/run.sh
#!/bin/bash

echo \$@
EOF

cat << EOF > docker/Dockerfile
FROM fedora:28

# Install the binaries
COPY run.sh /root/run.sh
RUN chmod +x /root/run.sh

CMD ["Hello"]
ENTRYPOINT ["/root/run.sh"]
EOF

docker rmi -f nessusio/test
docker build -t nessusio/test docker/

docker rm -f test
docker run --name test nessusio/test "Howdy World"
```
