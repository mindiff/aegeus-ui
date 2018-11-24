## Setup with Docker services

This type of installation requires a working [Docker](https://www.docker.com/community-edition#/download) environment.

In total there are four Docker images to make up the complete system.

1. [aegeus/aegeusd](https://hub.docker.com/r/aegeus/aegeusd)
2. [aegeus/aegeus-ipfs](https://hub.docker.com/r/aegeus/aegeus-ipfs)
3. [aegeus/aegeus-jaxrs](https://hub.docker.com/r/aegeus/aegeus-jaxrs)
4. [aegeus/aegeus-webui](https://hub.docker.com/r/aegeus/aegeus-webui)

What follows is an installation guide for all four containers. However, if you already have IPFS and AEG running locally, you will not need to run these in Docker again.
For a mixed setup with already running IPFS & AEG service and newly hosted Docker services go [here](Setup-Mixed-Docker.md).

For convenience however, lets do the whole setup in Docker first.

### Quickstart

Here is the quickstart to get the whole system running in no time ...

    export GATEWAYIP=[YOUR_PUBLIC_IP]

    docker run --detach --name aegd -p 29328:29328 --memory=300m --memory-swap=2g aegeus/aegeusd
    docker run --detach --name aeg-ipfs -p 4001:4001 -p 8080:8080 -e GATEWAYIP=$GATEWAYIP --memory=300m --memory-swap=2g aegeus/aegeus-ipfs
    docker run --detach --name aeg-jaxrs --link aegd:blockchain --link aeg-ipfs:ipfs --memory=100m --memory-swap=2g aegeus/aegeus-jaxrs
    docker run --detach --name aeg-webui -p 8082:8082 --link aegd:blockchain --link aeg-ipfs:ipfs --link aeg-jaxrs:jaxrs --memory=100m --memory-swap=2g --env NESSUS_WEBUI_LABEL=Bob aegeus/aegeus-webui

Now that everything is running, it should look similar to this

    docker ps

    CONTAINER ID        IMAGE                 COMMAND                  CREATED             STATUS              PORTS                                                      NAMES
    97b1fd2b0ac4        aegeus/aegeus-webui   "aegeus-webui"           6 seconds ago       Up 5 seconds        0.0.0.0:8082->8082/tcp                                     aeg-webui
    25c68fff1c7f        aegeus/aegeus-jaxrs   "aegeus-jaxrs start"     15 seconds ago      Up 14 seconds       8081/tcp                                                   aeg-jaxrs
    001daeec7fb9        aegeus/aegeus-ipfs    "nessusio-ipfs"          10 minutes ago      Up 10 minutes       0.0.0.0:4001->4001/tcp, 0.0.0.0:8080->8080/tcp, 5001/tcp   aeg-ipfs
    b9051f90b94c        aegeus/aegeusd        "aegeusd -datadir=..."   10 minutes ago      Up 10 minutes       0.0.0.0:29328->29328/tcp, 51473/tcp                        aegd

You should now be able to access the WebUI at: [http://127.0.0.1:8082/portal](http://127.0.0.1:8082/portal)

### Running the Aegeus daemon

To start the Aegeus daemon in Docker, you can run ...

    docker run --detach \
        -p 29328:29328 \
        --memory=300m --memory-swap=2g \
        --name aegd \
        aegeus/aegeusd

It'll take a little while for the network to sync. You can watch progress like this ...

    watch docker exec aegd aegeus-cli getinfo

### Running the Aegeus IPFS daemon

To start the Aegeus IPFS daemon in Docker, you can run ...

    export GATEWAYIP=192.168.178.20

    docker run --detach \
        -p 4001:4001 \
        -p 8080:8080 \
        --env GATEWAYIP=$GATEWAYIP \
        --memory=300m --memory-swap=2g \
        --name aeg-ipfs \
        aegeus/aegeus-ipfs

In case you need to connect the IPFS swarm to this instance, you can get the network ID like this ...

    echo "ipfs swarm connect /ip4/$GATEWAYIP/tcp/4001/ipfs/`docker exec aeg-ipfs ipfs config Identity.PeerID`"

and then on some other IPFS instance connect to the Aegeus IPFS daemon like this ...

    ipfs swarm connect /ip4/192.168.178.20/tcp/4001/ipfs/QmabAtE8qXJKDJ3SnxX18ZfEg9xMKdqoiA3KhW58hi4pmL

You can always get the system out for a running service like this ...

    docker logs aeg-ipfs

    initializing IPFS node at /root/.ipfs
    generating 2048-bit RSA keypair...done
    peer identity: QmabAtE8qXJKDJ3SnxX18ZfEg9xMKdqoiA3KhW58hi4pmL
    to get started, enter:

        ipfs cat /ipfs/QmS4ustL54uo8FzR9455qaxZwuMiUhyvMcX9Ba8nUH4uVv/readme

    Initializing daemon...
    Swarm listening on /ip4/127.0.0.1/tcp/4001
    Swarm listening on /ip4/172.17.0.2/tcp/4001
    Swarm listening on /p2p-circuit/ipfs/QmabAtE8qXJKDJ3SnxX18ZfEg9xMKdqoiA3KhW58hi4pmL
    Swarm announcing /ip4/127.0.0.1/tcp/4001
    Swarm announcing /ip4/172.17.0.2/tcp/4001
    API server listening on /ip4/0.0.0.0/tcp/5001
    Gateway (readonly) server listening on /ip4/0.0.0.0/tcp/8080
    Daemon is ready

### Running the AEG JAXRS image

This is the Aegeus JSON-RPC bridge, which contains the Aegeus application logic that connects the Aegeus network with IPFS network.

To start the Aegeus bridge in Docker, you can run ...

    docker run --detach \
        --link aegd:blockchain \
        --link aeg-ipfs:ipfs \
        --memory=100m --memory-swap=2g \
        --name aeg-jaxrs \
        aegeus/aegeus-jaxrs

On bootstrap the bridge reports some connection properties.

    docker logs aeg-jaxrs

    AegeusBlockchain: http://aeg:*******@172.17.0.3:51473
    AegeusNetwork Version: 3000000
    IPFS Version: 0.4.18
    Aegeus JAXRS: http://0.0.0.0:8081/aegeus

### Running the AEG WebUI image

This is a prototype of the Aegeus UI.

To start up the Aegeus UI in Docker, you can run ...

    export LABEL=Bob

    docker run --detach \
        -p 8082:8082 \
        --link aegd:blockchain \
        --link ipfs:ipfs \
        --link jaxrs:jaxrs \
        --env NESSUS_WEBUI_LABEL=$LABEL \
        --memory=100m --memory-swap=2g \
        --name aeg-webui \
        aegeus/aegeus-webui

The WebUI also reports some connection properties.

    docker logs webui

    AEG JAXRS: http://172.17.0.4:8081/aegeus
    IPFS Gateway: http://172.17.0.2:8080/ipfs
    AEG WebUI: http://0.0.0.0:8082/portal
    AegeusBlockchain: http://aeg:*******@172.17.0.3:51473
    AegeusNetwork Version: 2000000

You should now be able to access the WebUI at: [http://127.0.0.1:8082/portal](http://127.0.0.1:8082/portal)

### Working with JSON-RPC

Lets assume we don't want to use the WebUI, but instead use JSON-RPC calls only. Lets try ...

First, lets import a known AEG private key, so that Bob get some funds. This will take a while because the wallet needs to reindex the transactions.

    docker exec aegd aegeus-cli importprivkey "PRPS5pH***********************" Bob

On success, there is no feedback from this command.
When you again look at the WebUI, you should see that Bob's address is now assigned to an account and that he has some funds.

#### Knowing the Bridge IP address

    docker inspect --format='{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' aeg-jaxrs
    172.17.0.4

Then run a bash in a docker container like this

    docker run -it --rm fedora:29 bash

Note, we could also run the various commands below from the host's command line, if the bridge has published (and not just exposed) its main http port.

#### Running various JSON-RPC calls on the JAXRPC bridge

Lets register Bob's address with the system by making a JSON-RPC call to the bridge.

    curl http://172.17.0.4:8081/aegeus/register?addr=AJG36ywiYJnxJLdRDw8nzHUqKbAujmjvpX

    MDYwEAYHKoZIzj0CAQYFK4EEABwDIgAEP2ru5CB0CMyGX4sp8hz5qVn4eKgSo8NpToww2bAksZg=

This is Bob's public key used for IPFS file encryption.

Lets see, if we can also retrieve it from the blockchain.

    curl http://172.17.0.4:8081/aegeus/findkey?addr=AJG36ywiYJnxJLdRDw8nzHUqKbAujmjvpX

    MDYwEAYHKoZIzj0CAQYFK4EEABwDIgAEP2ru5CB0CMyGX4sp8hz5qVn4eKgSo8NpToww2bAksZg=

Now, lets add this document to the system.

    echo "Hello World" > test.txt
    curl --request POST --data @test.txt http://172.17.0.4:8081/aegeus/add?addr=AJG36ywiYJnxJLdRDw8nzHUqKbAujmjvpX\&path=test.txt

    {
        "owner":"AJG36ywiYJnxJLdRDw8nzHUqKbAujmjvpX",
        "path":"test.txt",
        "cid":"QmWjYT1TunvTPTRrAE3nVTLExV4e72EVpwS9JaqbH6ba59",
        "encrypted":true
    }

Again, this should be reflected in the WebUI.

Connecting to the IPFS gateway direcly, we should be able to see the file content.

    curl http://172.17.0.4:8080/ipfs/QmWjYT1TunvTPTRrAE3nVTLExV4e72EVpwS9JaqbH6ba59

    AEG-Version: 1.0
    Path: test.txt
    Owner: AJG36ywiYJnxJLdRDw8nzHUqKbAujmjvpX
    Token: BFv3LZ/mgq3VB0M8Y80GtF+t6TrRq4/FaG1dX3uqwghE1lEEnQTo3HkLTe907MRPP37eIgGxgJuIdb7OQxgI2G0fzxCy
    AEG_HEADER_END
    AAAADHGC7poPuTyDGLiytI9vHzrH2jgSWMZNGgzq8caW71n+OCognQBMKg==

Like above with the public encryption key, we should be able to find this IPFS content id on the blockhain.

    curl http://172.17.0.4:8081/aegeus/findipfs?addr=AJG36ywiYJnxJLdRDw8nzHUqKbAujmjvpX

    {
        "owner":"AJG36ywiYJnxJLdRDw8nzHUqKbAujmjvpX",
        "path":"test.txt",
        "cid":"QmWjYT1TunvTPTRrAE3nVTLExV4e72EVpwS9JaqbH6ba59",
        "encrypted":true
    }

The local unencrypted content is also available after the IPFS add.

    curl http://172.17.0.4:8081/aegeus/findlocal?addr=AJG36ywiYJnxJLdRDw8nzHUqKbAujmjvpX

    {
        "owner":"AJG36ywiYJnxJLdRDw8nzHUqKbAujmjvpX",
        "path":"test.txt",
        "cid":null,
        "encrypted":false}
    }

Lets delete that local file.

    curl http://172.17.0.4:8081/aegeus/rmlocal?addr=AJG36ywiYJnxJLdRDw8nzHUqKbAujmjvpX\&path=test.txt
    curl http://172.17.0.4:8081/aegeus/findlocal?addr=AJG36ywiYJnxJLdRDw8nzHUqKbAujmjvpX

    []

Lets assume at a later time, we would like to get that file from IPFS

    curl http://172.17.0.4:8081/aegeus/get?addr=AJG36ywiYJnxJLdRDw8nzHUqKbAujmjvpX\&path=other.txt\&cid=QmWjYT1TunvTPTRrAE3nVTLExV4e72EVpwS9JaqbH6ba59

    {
        "owner":"AJG36ywiYJnxJLdRDw8nzHUqKbAujmjvpX",
        "path":"other.txt",
        "cid":null,
        "encrypted":false}
    }

Finally, lets get the unencrypted content back

    curl http://172.17.0.4:8081/aegeus/getlocal?addr=AJG36ywiYJnxJLdRDw8nzHUqKbAujmjvpX\&path=other.txt

    Hello World

That's it - Enjoy!
