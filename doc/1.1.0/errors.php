<?php
/**
 * @apiDefine Error400
 * @apiError SYST-400 [HTTP 400] Invalid call to the service <code>xxx</code> (wrong parameters).
 */
/**
 * @apiDefine Error401
 * @apiError SYST-401 [HTTP 401] Authentication required.
 */
/**
 * @apiDefine Error402
 * @apiError SYST-402 [HTTP 402] Payment required.
 */
/**
 * @apiDefine Error404
 * @apiError SYST-404 [HTTP 404] Service <code>xxx</code> doesn't exist.
 */
/**
 * @apiDefine Error429
 * @apiError SYST-429 [HTTP 429] Too many request.
 */
/**
 * @apiDefine Error503
 * @apiError SYST-503 [HTTP 503] Service unavailable.
 */
 


/**
 * @apiDefine RGX001
 * @apiError RGX-001 Invalid phone number.
 */
/**
 * @apiDefine RGX002
 * @apiError RGX-002 Invalid confirmation code.
 */
/**
 * @apiDefine RGX003
 * @apiError RGX-003 Invalid nickname.
 */
/**
 * @apiDefine RGX004
 * @apiError RGX-004 Invalid gender.
 */
/**
 * @apiDefine RGX005
 * @apiError RGX-005 Invalid location.
 */
/**
 * @apiDefine RGX006
 * @apiError RGX-006 Invalid content type.
 */
/**
 * @apiDefine RGX007
 * @apiError RGX-007 Invalid file (doesn't correspond to content type).
 */
/**
 * @apiDefine RGX008
 * @apiError RGX-008 Invalid URL.
 */
/**
 * @apiDefine RGX009
 * @apiError RGX-009 Text is too short.
 */
/**
 * @apiDefine RGX010
 * @apiError RGX-010 Text is too long.
 */
/**
 * @apiDefine RGX011
 * @apiError RGX-011 Invalid URL's preview.
 */
/**
 * @apiDefine RGX012
 * @apiError RGX-012 Invalid URL's content type.
 */
/**
 * @apiDefine RGX013
 * @apiError RGX-013 Invalid rank.
 */
/**
 * @apiDefine RGX014
 * @apiError RGX-014 Title is too short.
 */
/**
 * @apiDefine RGX015
 * @apiError RGX-015 Title is too long.
 */


/**
 * @apiDefine PIC001
 * @apiError PIC-001 Invalid picture format.
 */
/**
 * @apiDefine PIC002
 * @apiError PIC-002 Invalid picture dimensions.
 */
/**
 * @apiDefine PIC003
 * @apiError PIC-003 Invalid picture size.
 */
/**
 * @apiDefine PIC101
 * @apiError PIC-101 Unable uploading picture (generic error).
 */



/**
 * @apiDefine TIME001
 * @apiError TIME-001   Invalid day number.
 */
/**
 * @apiDefine TIME002
 * @apiError TIME-002   Invalid week number.
 */
/**
 * @apiDefine TIME003
 * @apiError TIME-003   Invalid month number.
 */
/**
 * @apiDefine TIME004
 * @apiError TIME-004   Invalid year number.
 */
/**
 * @apiDefine TIME005
 * @apiError TIME-005   Invalid second number.
 */
/**
 * @apiDefine TIME006
 * @apiError TIME-006   Invalid minute number.
 */
/**
 * @apiDefine TIME007
 * @apiError TIME-007   Invalid hour number.
 */
/**
 * @apiDefine TIME008
 * @apiError TIME-008   Invalid date.
 */
/**
 * @apiDefine TIME009
 * @apiError TIME-009   Invalid time.
 */
/**
 * @apiDefine TIME010
 * @apiError TIME-010   Invalid timestamp.
 */


/**
 * @apiDefine USR001
 * @apiError USR-001 User <code>xxx</code> not found.
 */
/**
 * @apiDefine USR101
 * @apiError USR-101 Unable loading user <code>xxx</code> (generic error).
 */
/**
 * @apiDefine USR102
 * @apiError USR-102 Unable loading user <code>xxx</code>'s wall (generic error).
 */


/**
 * @apiDefine IDT001
 * @apiError IDT-001 Phone number already used.
 */
/**
 * @apiDefine IDT002
 * @apiError IDT-002 Suspended account.
 */
/**
 * @apiDefine IDT003
 * @apiError IDT-003 Deleted account.
 */
/**
 * @apiDefine IDT004
 * @apiError IDT-004 Wrong phone number.
 */
/**
 * @apiDefine IDT005
 * @apiError IDT-005 Wrong confirmation code (unrelated to this phone).
 */
/**
 * @apiDefine IDT007
 * @apiError IDT-007 Account not found.
 */
/**
 * @apiDefine IDT008
 * @apiError IDT-008 Invalid support message.
 */
/**
 * @apiDefine IDT009
 * @apiError IDT-009 Friend's list cannot be empty.
 */
/**
 * @apiDefine IDT101
 * @apiError IDT-101 Unable authenticating phone (generic error).
 */
/**
 * @apiDefine IDT102
 * @apiError IDT-102 Unable registering phone (generic error).
 */
/**
 * @apiDefine IDT103
 * @apiError IDT-103 Unable validating phone (generic error).
 */
/**
 * @apiDefine IDT104
 * @apiError IDT-104 Unable registering nickname (generic error).
 */
/**
 * @apiDefine IDT105
 * @apiError IDT-105 Unable unregistering phone (generic error).
 */
/**
 * @apiDefine IDT106
 * @apiError IDT-106 Unable sending changing phone number confirmation code (generic error).
 */
/**
 * @apiDefine IDT107
 * @apiError IDT-107 Unable changing phone number (generic error).
 */
/**
 * @apiDefine IDT108
 * @apiError IDT-108 Unable sending message to the support (generic error).
 */
/**
 * @apiDefine IDT109
 * @apiError IDT-109 Unable inviting friends (generic error).
 */


/**
 * @apiDefine SPK001
 * @apiError SPK-001 Spok <code>xxx</code> not found.
 */
/**
 * @apiDefine SPK002
 * @apiError SPK-002 Spok <code>xxx</code> is not available anymore.
 */
/**
 * @apiDefine SPK003
 * @apiError SPK-003 Media file (video, sound, picture or animated gif) is missing for creating the spok.
 */
/**
 * @apiDefine SPK004
 * @apiError SPK-004 Text's instance is too long.
 */
/**
 * @apiDefine SPK005
 * @apiError SPK-005 Answer <code>xxx</code> not found.
 */
/**
 * @apiDefine SPK006
 * @apiError SPK-006 Spok <code>xxx</code> already re-spoked.
 */
/**
 * @apiDefine SPK008
 * @apiError SPK-008 Comment <code>xxx</code> not found.
 */
/**
 * @apiDefine SPK009
 * @apiError SPK-009 Poll's questions have to be all answered before respoking spok <code>xxx</code>.
 */
/**
 * @apiDefine SPK010
 * @apiError SPK-010 Invalid answer to question <code>xxx</code>.
 */
/**
 * @apiDefine SPK011
 * @apiError SPK-011 Invalid spok poll's title.
 */
/**
 * @apiDefine SPK012
 * @apiError SPK-012 Invalid spok's questions.
 */
/**
 * @apiDefine SPK013
 * @apiError SPK-013 Invalid spok's answers.
 */
/**
 * @apiDefine SPK101
 * @apiError SPK-101 Unable loading spok <code>xxx</code> (generic error).
 */
/**
 * @apiDefine SPK102
 * @apiError SPK-102 Unable loading spok <code>xxx</code>'s re-spokers (generic error).
 */
/**
 * @apiDefine SPK103
 * @apiError SPK-103 Unable loading spok <code>xxx</code>'s scoped users (generic error).
 */
/**
 * @apiDefine SPK104
 * @apiError SPK-104 Unable loading spok <code>xxx</code>'s comments (generic error).
 */
/**
 * @apiDefine SPK105
 * @apiError SPK-105 Unable loading spok's stack (generic error).
 */
/**
 * @apiDefine SPK106
 * @apiError SPK-106 Unable creating spok (generic error).
 */
/**
 * @apiDefine SPK107
 * @apiError SPK-107 Unable uploading file (generic error).
 */
/**
 * @apiDefine SPK108
 * @apiError SPK-108 Unable publishing spok (generic error).
 */
/**
 * @apiDefine SPK109
 * @apiError SPK-109 Unable creating poll (generic error).
 */
/**
 * @apiDefine SPK112
 * @apiError SPK-112 Unable viewing question <code>yyy</code> of the spok poll <code>xxx</code> (generic error).
 */
/**
 * @apiDefine SPK115
 * @apiError SPK-115 Unable disabling spok <code>xxx</code> (generic error).
 */
/**
 * @apiDefine SPK116
 * @apiError SPK-116 Unable removing spok <code>xxx</code> (generic error).
 */
/**
 * @apiDefine SPK117
 * @apiError SPK-117 Unable re-spoking spok <code>xxx</code> (generic error).
 */
/**
 * @apiDefine SPK118
 * @apiError SPK-118 Unable un-spoking spok <code>xxx</code> (generic error).
 */
/**
 * @apiDefine SPK119
 * @apiError SPK-119 Unable commenting spok <code>xxx</code> (generic error).
 */
/**
 * @apiDefine SPK120
 * @apiError SPK-120 Unable updating comment <code>xxx</code> (generic error).
 */
/**
 * @apiDefine SPK121
 * @apiError SPK-121 Unable removing comment <code>xxx</code> (generic error).
 */
/**
 * @apiDefine SPK122
 * @apiError SPK-122 Unable loading spok <code>xxx</code>'s comments (generic error).
 */
/**
 * @apiDefine SPK123
 * @apiError SPK-123 Unable susbcribing to / unsubscribing from spok <code>xxx</code>'s feed (generic error).
 */
/**
 * @apiDefine SPK124
 * @apiError SPK-124 Unable saving answer to poll's question <code>xxx</code> (generic error).
 */


/**
 * @apiDefine MSG001
 * @apiError MSG-001 Message cannot be empty.
 */
/**
 * @apiDefine MSG002
 * @apiError MSG-002 Talk <code>xxx</code> not found.
 */
/**
 * @apiDefine MSG003
 * @apiError MSG-003 Message <code>xxx</code> not found.
 */
/**
 * @apiDefine MSG101
 * @apiError MSG-101 Unable loading talks list (generic error).
 */
/**
 * @apiDefine MSG102
 * @apiError MSG-102 Unable loading talk's messages (generic error).
 */
/**
 * @apiDefine MSG103
 * @apiError MSG-103 Unable sending message (generic error).
 */
/**
 * @apiDefine MSG104
 * @apiError MSG-104 Unable removing talk (generic error).
 */
/**
 * @apiDefine MSG105
 * @apiError MSG-105 Unable removing message (generic error).
 */


/**
 * @apiDefine MYA001
 * @apiError MYA-001 Notification <code>xxx</code> not found.
 */
/**
 * @apiDefine MYA101
 * @apiError MYA-101 Unable updating profile (generic error).
 */
/**
 * @apiDefine MYA102
 * @apiError MYA-102 Unable loading notifications (generic error).
 */
/**
 * @apiDefine MYA103
 * @apiError MYA-103 Unable removing notification <code>xxx</code> (generic error).
 */
/**
 * @apiDefine MYA104
 * @apiError MYA-104 Unable updating help setting (generic error).
 */
/**
 * @apiDefine MYA105
 * @apiError MYA-105 Unable updating follows setting (generic error).
 */


/**
 * @apiDefine GRP001
 * @apiError GRP-001 Group <code>xxx</code> not found.
 */
/**
 * @apiDefine GRP101
 * @apiError GRP-101 Unable creating group <code>xxx</code> (generic error).
 */
/**
 * @apiDefine GRP102
 * @apiError GRP-102 Unable updating group <code>xxx</code> (generic error).
 */
/**
 * @apiDefine GRP103
 * @apiError GRP-103 Unable removing group <code>xxx</code> (generic error).
 */
/**
 * @apiDefine GRP104
 * @apiError GRP-104 Unable listing groups (generic error).
 */
/**
 * @apiDefine GRP105
 * @apiError GRP-105 Unable adding user(s) or contact(s) to group <code>xxx</code> (generic error).
 */
/**
 * @apiDefine GRP106
 * @apiError GRP-106 Unable removing user(s) or contact(s) from group <code>xxx</code> (generic error).
 */


/**
 * @apiDefine FLW001
 * @apiError FLW-001 Not allowed to load user <code>xxx</code>'s followers.
 */
/**
 * @apiDefine FLW002
 * @apiError FLW-002 Not allowed to load user <code>xxx</code>'s followings.
 */
/**
 * @apiDefine FLW101
 * @apiError FLW-101 Unable follow/unfollow user <code>xxx</code> (generic error).
 */
/**
 * @apiDefine FLW102
 * @apiError FLW-102 Unable loading user <code>xxx</code>'s followers (generic error).
 */
/**
 * @apiDefine FLW103
 * @apiError FLW-103 Unable loading user <code>xxx</code>'s followings (generic error).
 */




/**
 * @apiDefine SRH001
 * @apiError SRH-001 Invalid nicknames.
 */
/**
 * @apiDefine SRH002
 * @apiError SRH-002 Invalid hashtags.
 */
/**
 * @apiDefine SRH003
 * @apiError SRH-003 Invalid nickname.
 */
/**
 * @apiDefine SRH004
 * @apiError SRH-004 Invalid hashtag.
 */
/**
 * @apiDefine SRH005
 * @apiError SRH-005 Invalid location.
 */
/**
 * @apiDefine SRH101
 * @apiError SRH-101 Unable loading popular spokers list (generic error).
 */
/**
 * @apiDefine SRH102
 * @apiError SRH-102 Unable loading trendy spoks list (generic error).
 */
/**
 * @apiDefine SRH103
 * @apiError SRH-103 Unable loading the last spoks of my friends (generic error).
 */
/**
 * @apiDefine SRH104
 * @apiError SRH-104 Unable loading the last spoks list (generic error).
 */
/**
 * @apiDefine SRH105
 * @apiError SRH-105 Unable searching spoks (generic error).
 */
/**
 * @apiDefine SRH106
 * @apiError SRH-106 Unable searching nicknames (generic error).
 */
/**
 * @apiDefine SRH107
 * @apiError SRH-107 Unable searching hashtags (generic error).
 */
/**
 * @apiDefine SRH108
 * @apiError SRH-108 Unable searching locations (generic error).
 */
