# dep-align

when completed this tool should allow to:
- point it at a mvn repo
- give it a bunch of groupId/artifactId pairs
- get graph of dependencies *with version details*
- tell you what is latest available version of every dep

## now...

just a rough sketch. If you want to try:
- hack `MainTest` to specify your ids
- run test
- open `/tmp/dep-align.html` w/ your browser

## why another graph tool?

there are plenty around. from a quick search I couldn't find one that:
- operates from multiple dependency roots at once
- gives a nice viz of version alignment information (e.g. nb shows it on arrows plus yellow/red for conflicts)
- discovers latest (and maybe all other?) available versions

## again, why????

I have in mind the specific use case of maintaining a bunch of (internal?) libraries that possibly depend on each other and the pain of having to hand-draw the graph everytime.

No, I don't believe parent poms are a solution, tend to tie everything together rather than promoting decupling.

Yes, force auto-bump everything on the CI system is a possible solution, not always available and not always desired (depending on how the ownership model of your libraries is and how you perceive them).

## this already exist.

I couldn't find it. Please get in touch and tell me, I'd love to avoid reimplementing it!

### signing

all my commits are signed with this PGP key [6897 DE5F D543 F3DA 02C1  3FEA 7AD2 E918 B3D5 FFB7](https://pgp.mit.edu/pks/lookup?op=get&search=0x7AD2E918B3D5FFB7)
