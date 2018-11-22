## Build the AEG daemon image

### Build the Aegeus daemon

```
export NVERSION=3.0

# Fetch the bootstrap data
wget -O bootstrap.tgz http://ipfsgw1.aegeus.io/QmTKM3LKJndwwvS6Aprc5Ptkz9DGy6PTSErJagqBmTfXw7

rm -rf docker
mkdir docker

# Copy bootstrap data to the build dir
mkdir docker/aegeusd
tar -C docker/aegeusd -xzf bootstrap.tgz

export RPCUSER=aeg
export RPCPASS=aegpass
export RPCPORT=51473

cat << EOF > docker/aegeus-server.conf
rpcuser=$RPCUSER
rpcpassword=$RPCPASS
rpcport=$RPCPORT
rpcallowip=172.17.0.1/24
listen=1
server=1
EOF

cat << EOF > docker/aegeus-client.conf
rpcuser=$RPCUSER
rpcpassword=$RPCPASS
rpcport=$RPCPORT
EOF

cat << EOF > docker/Dockerfile
# Based in Ubuntu 16.04
FROM ubuntu:16.04

# System update/upgrade
RUN apt -y update
RUN apt -y upgrade
RUN apt -y install python3 software-properties-common
RUN apt-add-repository -y ppa:bitcoin/bitcoin
RUN apt -y update
RUN apt -y upgrade
RUN apt -y install git make software-properties-common build-essential libtool autoconf libssl-dev libboost-dev libboost-chrono-dev libboost-filesystem-dev libboost-program-options-dev libboost-system-dev libboost-test-dev libboost-thread-dev automake git wget curl libdb4.8-dev bsdmainutils libdb4.8++-dev libminiupnpc-dev libgmp3-dev ufw pkg-config libevent-dev libdb5.3++ unzip libzmq5

# Checkout the tagged version
RUN git clone https://github.com/AegeusCoin/aegeus.git

# Build aegeusd
WORKDIR aegeus

RUN git checkout $NVERSION
RUN ./autogen.sh
RUN ./configure
RUN make
RUN strip src/aegeusd

# Install the binaries
RUN cp src/aegeusd /usr/bin/
RUN cp src/aegeus-cli /usr/bin/

# Cleanup the build
WORKDIR ..
RUN rm -rf aegeus

# Install the config files
COPY aegeus-server.conf /etc/aegeusd/aegeus.conf
COPY aegeus-client.conf /root/.aegeus/aegeus.conf

# Copy bootstrap data
RUN mkdir /var/lib/aegeusd
COPY aegeusd /var/lib/aegeusd

# Set some default env vars
ENV RPCUSER=$RPCUSER
ENV RPCPASS=$RPCPASS

# Expose the API port
EXPOSE $RPCPORT

# Use the daemon as entry point
ENTRYPOINT ["aegeusd", "-datadir=/var/lib/aegeusd", "-conf=/etc/aegeusd/aegeus.conf"]
EOF

docker build -t aegeus/aegeusd docker/
docker push aegeus/aegeusd

docker tag aegeus/aegeusd aegeus/aegeusd:$NVERSION
docker push aegeus/aegeusd:$NVERSION
```

### Run the Aegeus daemon

```
export CNAME=aegd

docker rm -f $CNAME
docker run --detach \
    -p 29328:29328 \
    --memory=300m --memory-swap=2g \
    --name $CNAME \
    aegeus/aegeusd

watch docker exec aegd aegeus-cli getinfo

docker exec aegd tail -f /var/lib/aegeusd/debug.log
```
