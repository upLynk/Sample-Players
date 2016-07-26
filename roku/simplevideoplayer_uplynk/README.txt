This is a copy of Roku's simplevideoplayer sample customized for
playing content from Uplynk's servers.
Documentation here: http://support.uplynk.com/tut_roku_test.html

The simplevideoplayer example is the barebones 
application that plays a video. It has hardcoded 
content parameters that allow for quick modification
to test any of your own content for playablility.

If you want to quickly test your own content: open
the source/appMain.brs file; in the function
displayVideo(), change the urls, bitrates, qualities, 
and streamformat to match your content.

With the modifed script pointing to your content, load
the channel and navigate to the "Play" button. When
you hit "Play" you can view your video playing on 
the Roku DVP.

Please see Section 4.5 of the Component Reference Guide
for more information on the Video Screen Object and 
Section 3.3 for more information on setting the 
content meta-data parameters about your video.

**************************************************************
This example uses the Big Buck Bunny video streamed from Uplynk servers 
Big Buck Bunny is open source content created by the Blender Foundation.

Please see the following for license details:
http://www.bigbuckbunny.org/index.php/about/







