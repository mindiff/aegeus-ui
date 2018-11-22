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

cat << EOF > docker/aegeus-server.conf
rpcuser=aeg
rpcpassword=aegpass
rpcport=51473
rpcallowip=127.0.0.1
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

# Expose the API port
EXPOSE 51473

# Set some default env vars
ENV RPCUSER=aeg
ENV RPCPASS=aegpass

# Use the daemon as entry point
ENTRYPOINT ["aegeusd", "-datadir=/var/lib/aegeusd", "-conf=/etc/aegeusd/aegeus.conf"]
EOF

docker rmi -f aegeus/aegeusd
docker build -t aegeus/aegeusd:$NVERSION docker/

docker tag aegeus/aegeusd:$NVERSION aegeus/aegeusd
docker push aegeus/aegeusd:$NVERSION


### Run the Aegeus daemon

```
export MNNAME=aegd

docker rm -f $MNNAME
docker run --detach \
    -p 29328:29328 \
    --memory=200m --memory-swap=2g \
    --name $MNNAME \
    aegeus/aegeusd

watch docker exec $MNNAME aegeus-cli getinfo

docker exec $MNNAME tail -f /var/lib/aegeusd/debug.log
```
