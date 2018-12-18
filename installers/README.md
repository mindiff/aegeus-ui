### Welcome to the Aegeus UI public beta test!

It's important to note that this is a beta test and not a release candidate and has met all milestones aside from the portable executable as can be seen here: [1.0.0 Beta1 Milestones](https://github.com/AegeusCoin/aegeus-ui/milestone/2).  As everyone finds any issues they'd like to report or has a request for a feature, they can go to [Aegeus UI Issues](https://github.com/AegeusCoin/aegeus-ui/issues) and create a new issue.  1.0.0 Beta2 milestones can be viewed at [1.0.0 Beta2 Milestones](https://github.com/AegeusCoin/aegeus-ui/milestone/3) and monitored as we'll be adding more there as we move forward with development.  The [Release Notes](https://github.com/AegeusCoin/aegeus-ui/blob/master/docs/ReleaseNotes.md) and the [ChangeLog](https://github.com/AegeusCoin/aegeus-ui/blob/master/docs/Changelog.md) are also available for your viewing.

### About this system

Our system is designed uniquely to ensure constant availability with your data's privacy in-tact at all times. No private corporations or governments can access this data without you taking specific action to grant them access. All data once added into this system is encrypted with a strong encryption key and stored on distributed network storage. When you wish to share content with another party, your data is encrypted again such that the other party and only the other party can access that data and view the unencrypted contents.  We have much planned and limits stretch as far as your imagination virtually in terms of application.

### What you'll need to use it

* A VPS (that isn't currently running an Aegeus wallet) with at least 2 gigs of RAM
* Some AEG (which we can provide if you contact us on discord)
* Patience and understanding that this is a public beta release and you may experience some tiny glitches

### Let's get started!

* SSH into your VPS as root or sudo to root (the latter is a more secure and preferred way)
* Type this on the commandline: ``which curl`` If you don't see something like /usr/bin/curl type this: ``apt -y install curl``
* Type this on the commandline: ``bash <(curl https://raw.githubusercontent.com/AegeusCoin/aegeus-ui/master/installers/aegui-installer.sh)``

This will begin installing dependencies and additional software required for this beta as well as all of the components for it.

Once it's complete, you will see a message like this:

> Congratulations.  Installation is complete.  Please visit http://x.x.x.x:8082/portal in your browser of choice.

x.x.x.x is just a placeholder for your actual IP, which will be displayed after installation is complete.

### Next steps

1. Click on addresses and generate a new address.  Give it a name and click ``Generate New Address``

![](images/step1.jpg?raw=true)

Now you will see it has been added into your portal.

![](images/step1-result.jpg?raw=true)

2. Send this new address some AEG or ask us to on discord.  It doesn't need much, send 1 AEG to it.  Once you have sent the AEG, refresh the addresses page and you should see a balance along with a button ``register`` next to it.  Click ``register``

![](images/step2.jpg?raw=true)

Now you will see your new address has been registered and a small amount of AEG for this process has been spent.

![](images/step2-result.jpg?raw=true)

3. Click on the ``Files`` menu and ``Create Document``.  Give it a unique filename and type in whatever content you'd like to share securely with another person, then click ``Add Content``

![](images/step3.jpg?raw=true)

4. Another person using this system that you'd like to share a file with at this point should share their registered address with you.  Once they have, click the ``Addresses`` menu and then ``Address Book``.  Give the address you're adding to your book a label that you'll be familiar with and paste their registered AEG address into the input box to the right and click ``Import Key`` This can take some time, as it is scanning the blockchain for this user's key information.  This is a method that is going to be vastly improved upon in the upcoming release.

![](images/step4.jpg?raw=true)

Now as you can see their key information and address is imported into your portal.

![](images/step4-result.jpg?raw=true)

5. Click ``IPFS Files`` and you will see the file you just created in step 3.  Click ``send`` and you will be given a choice of recipient if you have more than one.  Choose the person to share with and click ``send``.  You will be taken back to your list of files on the IPFS network.  Content stored is encrypted and no one but you or intended parties will be able to view it.

![](images/step5.jpg?raw=true)

What you're viewing now is what the rest of the world would see if they tried to access your data.  They would be unable to view it unless you specifically encrypted it using their key.

![](images/step5-result.jpg?raw=true)

6. The person you are sharing with can now click the ``Files`` menu and then the AEG address they shared with you, then ``IPFS Files`` to view files encrypted using that key.
7. This system can also fetch remote documents and import them for you to save, access and share at any time.  Click ``Add By URL`` and give the content a label and type or paste the URL into the ``Content URL`` box.

![](images/step7.jpg?raw=true)

Now this imported content is available for your viewing or sharing in your portal.

![](images/step7-result.jpg?raw=true)

As you can see, the rest of the world only sees encrypted bits.  Only you, and those you choose to share with can view the actual content.

![](images/step7-result2.jpg?raw=true)

One of the more exciting additions to the next release is the ability to add full directories and binary content.  We will also be working in the meantime on building out our in-wallet system and storage network with monetization options for being a storage provider (AEG Storage Node) or a storage client (Renting storage from those within the network).  We hope you've enjoyed this beta release and if there are any questions you can join us on Discord https://discord.gg/pwn4pBA

