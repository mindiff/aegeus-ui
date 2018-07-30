## Build the AEG IPFS image

### Build the Golang image 

```
rm -rf docker
mkdir -p docker

cat << EOF > docker/Dockerfile
FROM fedora:28

RUN dnf -y install golang

CMD ["version"]

ENTRYPOINT ["go"]
EOF

docker rmi -f aegeus/golang
docker build -t aegeus/golang docker/

docker run --rm aegeus/golang

docker push aegeus/golang

docker tag aegeus/golang aegeus/golang:1.10.3
docker push aegeus/golang:1.10.3
```

### Build the IPFS image

This is largly inspired by [Run IPFS latest on a VPS](https://ipfs.io/blog/22-run-ipfs-on-a-vps/).

```
export IPFS_VERSION=0.4.16
export IPFS_PLATFORM=linux-386
wget --no-check-certificate https://dist.ipfs.io/go-ipfs/v$IPFS_VERSION/go-ipfs_v"$IPFS_VERSION"_"$IPFS_PLATFORM".tar.gz

rm -rf docker
mkdir -p docker

tar xzf go-ipfs_*.tar.gz -C docker/

export RUNNAME=run-aegeus-ipfs.sh
cat << EOF > docker/$RUNNAME
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
FROM aegeus/golang

COPY go-ipfs/ipfs /usr/local/bin/

COPY $RUNNAME /root/$RUNNAME
RUN chmod +x /root/$RUNNAME

# Make the entrypoint executable
RUN ln -s /root/$RUNNAME /usr/local/bin/aegeus-ipfs

ENTRYPOINT ["aegeus-ipfs"]
EOF

docker rmi -f aegeus/aegeus-ipfs
docker build -t aegeus/aegeus-ipfs docker/

docker push aegeus/aegeus-ipfs

docker tag aegeus/aegeus-ipfs aegeus/aegeus-ipfs:$IPFS_VERSION
docker push aegeus/aegeus-ipfs:$IPFS_VERSION
```

### Run the IPFS image 

```
export NAME=aeg-ipfs

docker rm -f $NAME
docker run --detach \
    -p 4001:4001 \
    -p 8080:8080 \
    --expose 5001 \
    --memory=200m --memory-swap=2g \
    --name $NAME \
    aegeus/aegeus-ipfs

docker logs $NAME
```

### Build the IPFS Java image

```
rm -rf docker
mkdir -p docker

cat << EOF > docker/Dockerfile
FROM aegeus/aegeus-ipfs

# Install dependencies
RUN dnf -y install java

CMD ["-version"]
ENTRYPOINT ["java"]
EOF

docker rmi -f aegeus/aegeus-ipfsj
docker build -t aegeus/aegeus-ipfsj docker/

docker run --rm aegeus/aegeus-ipfsj -version

docker push aegeus/aegeus-ipfsj

docker tag aegeus/aegeus-ipfsj aegeus/aegeus-ipfsj:$IPFS_VERSION
docker push aegeus/aegeus-ipfsj:$IPFS_VERSION
```

