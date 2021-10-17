import './App.css';
import {Component} from "react";
import AnyChart from 'anychart-react'
import anychart from 'anychart'

import localMap from './data/local'
import partyColor from './data/partyColor'

let rawData = [{"id":"0000","totalVotes":577382,"leadingParty":"KESK","partyData":[{"name":"KESK","votes":157928},{"name":"VAL_LIIDUD","votes":155341},{"name":"REF","votes":112755},{"name":"SDE","votes":59309},{"name":"IRL","votes":45195},{"name":"EKRE","votes":38501},{"name":"ROH","votes":4669},{"name":"üksik","votes":2557},{"name":"EÜVP","votes":1127}]},{"id":"0037","totalVotes":70331,"leadingParty":"VAL_LIIDUD","partyData":[{"name":"VAL_LIIDUD","votes":28528},{"name":"REF","votes":17416},{"name":"KESK","votes":9071},{"name":"IRL","votes":5737},{"name":"SDE","votes":4320},{"name":"EKRE","votes":4293},{"name":"EÜVP","votes":492},{"name":"üksik","votes":474}]},{"id":"0039","totalVotes":4830,"leadingParty":"VAL_LIIDUD","partyData":[{"name":"VAL_LIIDUD","votes":2330},{"name":"SDE","votes":1255},{"name":"REF","votes":501},{"name":"EKRE","votes":291},{"name":"KESK","votes":286},{"name":"IRL","votes":167}]},{"id":"0045","totalVotes":55361,"leadingParty":"KESK","partyData":[{"name":"KESK","votes":29311},{"name":"VAL_LIIDUD","votes":19327},{"name":"REF","votes":3253},{"name":"SDE","votes":2980},{"name":"IRL","votes":372},{"name":"üksik","votes":70},{"name":"ROH","votes":48}]},{"id":"0052","totalVotes":9266,"leadingParty":"IRL","partyData":[{"name":"IRL","votes":2446},{"name":"SDE","votes":1750},{"name":"VAL_LIIDUD","votes":1715},{"name":"REF","votes":1607},{"name":"EKRE","votes":1045},{"name":"KESK","votes":703}]},{"id":"0050","totalVotes":14828,"leadingParty":"VAL_LIIDUD","partyData":[{"name":"VAL_LIIDUD","votes":4443},{"name":"KESK","votes":3110},{"name":"REF","votes":2697},{"name":"SDE","votes":2609},{"name":"IRL","votes":1104},{"name":"EKRE","votes":854},{"name":"üksik","votes":11}]},{"id":"0056","totalVotes":10108,"leadingParty":"VAL_LIIDUD","partyData":[{"name":"VAL_LIIDUD","votes":5922},{"name":"KESK","votes":1276},{"name":"SDE","votes":1108},{"name":"REF","votes":955},{"name":"EKRE","votes":840},{"name":"EÜVP","votes":7}]},{"id":"0060","totalVotes":26932,"leadingParty":"VAL_LIIDUD","partyData":[{"name":"VAL_LIIDUD","votes":11303},{"name":"KESK","votes":4696},{"name":"REF","votes":4538},{"name":"IRL","votes":3269},{"name":"EKRE","votes":1633},{"name":"SDE","votes":1360},{"name":"üksik","votes":123},{"name":"EÜVP","votes":10}]},{"id":"0068","totalVotes":36479,"leadingParty":"VAL_LIIDUD","partyData":[{"name":"VAL_LIIDUD","votes":17120},{"name":"KESK","votes":5142},{"name":"REF","votes":4604},{"name":"EKRE","votes":4329},{"name":"IRL","votes":3860},{"name":"SDE","votes":1096},{"name":"üksik","votes":328}]},{"id":"0064","totalVotes":12411,"leadingParty":"REF","partyData":[{"name":"REF","votes":4413},{"name":"KESK","votes":2823},{"name":"VAL_LIIDUD","votes":2669},{"name":"SDE","votes":1226},{"name":"EKRE","votes":1162},{"name":"üksik","votes":118}]},{"id":"0071","totalVotes":13980,"leadingParty":"VAL_LIIDUD","partyData":[{"name":"VAL_LIIDUD","votes":7584},{"name":"EKRE","votes":2317},{"name":"IRL","votes":1766},{"name":"REF","votes":925},{"name":"KESK","votes":852},{"name":"SDE","votes":498},{"name":"üksik","votes":38}]},{"id":"0074","totalVotes":14305,"leadingParty":"VAL_LIIDUD","partyData":[{"name":"VAL_LIIDUD","votes":3717},{"name":"SDE","votes":3563},{"name":"REF","votes":3229},{"name":"IRL","votes":1313},{"name":"KESK","votes":1090},{"name":"EKRE","votes":950},{"name":"üksik","votes":443}]},{"id":"0079","totalVotes":24790,"leadingParty":"VAL_LIIDUD","partyData":[{"name":"VAL_LIIDUD","votes":15049},{"name":"REF","votes":4846},{"name":"SDE","votes":1884},{"name":"IRL","votes":1365},{"name":"KESK","votes":1303},{"name":"EKRE","votes":260},{"name":"üksik","votes":83}]},{"id":"0081","totalVotes":13977,"leadingParty":"REF","partyData":[{"name":"REF","votes":4501},{"name":"KESK","votes":3631},{"name":"SDE","votes":2322},{"name":"VAL_LIIDUD","votes":1937},{"name":"IRL","votes":928},{"name":"EKRE","votes":658}]},{"id":"0084","totalVotes":21173,"leadingParty":"IRL","partyData":[{"name":"IRL","votes":5353},{"name":"VAL_LIIDUD","votes":4364},{"name":"REF","votes":4144},{"name":"SDE","votes":3380},{"name":"KESK","votes":2465},{"name":"EKRE","votes":1460},{"name":"üksik","votes":7}]},{"id":"0087","totalVotes":17962,"leadingParty":"VAL_LIIDUD","partyData":[{"name":"VAL_LIIDUD","votes":9875},{"name":"SDE","votes":2458},{"name":"IRL","votes":1999},{"name":"KESK","votes":1655},{"name":"REF","votes":1333},{"name":"EKRE","votes":642}]},{"id":"0784","totalVotes":191925,"leadingParty":"KESK","partyData":[{"name":"KESK","votes":85199},{"name":"REF","votes":39323},{"name":"SDE","votes":21053},{"name":"EKRE","votes":13437},{"name":"IRL","votes":12638},{"name":"SVL TT","votes":8562},{"name":"VTK VL","votes":5044},{"name":"ROH","votes":4621},{"name":"üksik","votes":698},{"name":"EÜVP","votes":592},{"name":"VL MEIET","votes":240},{"name":"VL ML","votes":224},{"name":"VL EEJL","votes":186},{"name":"VL Hääled","votes":108}]},{"id":"0793","totalVotes":38724,"leadingParty":"REF","partyData":[{"name":"REF","votes":14470},{"name":"SDE","votes":6447},{"name":"KESK","votes":5315},{"name":"EKRE","votes":4330},{"name":"IRL","votes":2878},{"name":"VTH","votes":2808},{"name":"VTE","votes":2086},{"name":"VST","votes":200},{"name":"üksik","votes":164},{"name":"EÜVP","votes":26}]},{"id":"0130","totalVotes":2440,"leadingParty":"VLAH","partyData":[{"name":"VLAH","votes":970},{"name":"REF","votes":589},{"name":"VLOV","votes":565},{"name":"KESK","votes":265},{"name":"üksik","votes":36},{"name":"IRL","votes":15}]},{"id":"0141","totalVotes":2489,"leadingParty":"VAAÜ","partyData":[{"name":"VAAÜ","votes":1328},{"name":"KESK","votes":701},{"name":"REF","votes":385},{"name":"IRL","votes":75}]},{"id":"0142","totalVotes":2361,"leadingParty":"VL AU","partyData":[{"name":"VL AU","votes":904},{"name":"VL MES","votes":745},{"name":"VL ÜJ","votes":530},{"name":"KESK","votes":182}]},{"id":"0171","totalVotes":6728,"leadingParty":"REF","partyData":[{"name":"REF","votes":2135},{"name":"VLEE","votes":1442},{"name":"VLEVK","votes":1185},{"name":"SDE","votes":979},{"name":"KESK","votes":576},{"name":"VLE","votes":411}]},{"id":"0184","totalVotes":6365,"leadingParty":"HARI","partyData":[{"name":"HARI","votes":2825},{"name":"KESK","votes":1135},{"name":"Meie Inimesed","votes":948},{"name":"REF","votes":637},{"name":"EKRE","votes":449},{"name":"SDE","votes":364},{"name":"EÜVP","votes":7}]},{"id":"0191","totalVotes":2176,"leadingParty":"REF","partyData":[{"name":"REF","votes":464},{"name":"VLVVK","votes":429},{"name":"VLM","votes":425},{"name":"VLPA","votes":322},{"name":"KESK","votes":219},{"name":"EKRE","votes":188},{"name":"IRL","votes":121},{"name":"üksik","votes":8}]},{"id":"0198","totalVotes":6160,"leadingParty":"IRL","partyData":[{"name":"IRL","votes":2025},{"name":"REF","votes":1531},{"name":"HL","votes":1128},{"name":"EKRE","votes":680},{"name":"KESK","votes":417},{"name":"SDE","votes":325},{"name":"üksik","votes":54}]},{"id":"0205","totalVotes":4830,"leadingParty":"SDE","partyData":[{"name":"SDE","votes":1255},{"name":"VLÜH","votes":1230},{"name":"VLK","votes":1100},{"name":"REF","votes":501},{"name":"EKRE","votes":291},{"name":"KESK","votes":286},{"name":"IRL","votes":167}]},{"id":"0214","totalVotes":2424,"leadingParty":"VLKT","partyData":[{"name":"VLKT","votes":790},{"name":"VLÜK","votes":625},{"name":"VLRT","votes":559},{"name":"KESK","votes":240},{"name":"EKRE","votes":168},{"name":"IRL","votes":42}]},{"id":"0255","totalVotes":4318,"leadingParty":"REF","partyData":[{"name":"REF","votes":970},{"name":"JVHVL","votes":769},{"name":"SJVL","votes":765},{"name":"EKRE","votes":623},{"name":"IRL","votes":542},{"name":"KESK","votes":358},{"name":"SDE","votes":291}]},{"id":"0245","totalVotes":2828,"leadingParty":"REF","partyData":[{"name":"REF","votes":1586},{"name":"SDE","votes":465},{"name":"EKRE","votes":316},{"name":"KESK","votes":238},{"name":"","votes":201},{"name":"VLEEL","votes":22}]},{"id":"0247","totalVotes":6936,"leadingParty":"KESK","partyData":[{"name":"KESK","votes":1950},{"name":"REF","votes":1433},{"name":"IRL","votes":899},{"name":"VLUJ","votes":838},{"name":"SDE","votes":543},{"name":"EKRE","votes":438},{"name":"VLVRM","votes":428},{"name":"VLKK","votes":397},{"name":"üksik","votes":10}]},{"id":"0251","totalVotes":5438,"leadingParty":"VLJMK","partyData":[{"name":"VLJMK","votes":1592},{"name":"VLJE","votes":1444},{"name":"VLJ","votes":904},{"name":"KESK","votes":855},{"name":"REF","votes":503},{"name":"SDE","votes":140}]},{"id":"0272","totalVotes":2348,"leadingParty":"VLTV","partyData":[{"name":"VLTV","votes":1006},{"name":"VLPK","votes":581},{"name":"VLK","votes":421},{"name":"IRL","votes":176},{"name":"KESK","votes":164}]},{"id":"0283","totalVotes":4617,"leadingParty":"REF","partyData":[{"name":"REF","votes":2474},{"name":"VLKV","votes":1316},{"name":"SDE","votes":412},{"name":"EKRE","votes":260},{"name":"KESK","votes":155}]},{"id":"0284","totalVotes":2474,"leadingParty":"REF","partyData":[{"name":"REF","votes":1573},{"name":"KESK","votes":731},{"name":"üksik","votes":94},{"name":"EKRE","votes":76}]},{"id":"0291","totalVotes":2325,"leadingParty":"VL MK","partyData":[{"name":"VL MK","votes":1702},{"name":"VL TÕ","votes":474},{"name":"KESK","votes":149}]},{"id":"0293","totalVotes":2235,"leadingParty":"","partyData":[{"name":"","votes":714},{"name":"","votes":503},{"name":"EKRE","votes":364},{"name":"SDE","votes":329},{"name":"KESK","votes":319},{"name":"IRL","votes":6}]},{"id":"0296","totalVotes":4105,"leadingParty":"REF","partyData":[{"name":"REF","votes":2294},{"name":"VSK","votes":906},{"name":"SDE","votes":417},{"name":"KESK","votes":289},{"name":"IRL","votes":199}]},{"id":"0303","totalVotes":383,"leadingParty":"VL PK","partyData":[{"name":"VL PK","votes":277},{"name":"üksik","votes":106}]},{"id":"0305","totalVotes":2461,"leadingParty":"VKKÜ","partyData":[{"name":"VKKÜ","votes":1370},{"name":"VKAR","votes":799},{"name":"REF","votes":180},{"name":"EKRE","votes":112}]},{"id":"0317","totalVotes":2721,"leadingParty":"IRL","partyData":[{"name":"IRL","votes":1439},{"name":"VLPK","votes":887},{"name":"EKRE","votes":348},{"name":"VLAK","votes":47}]},{"id":"0321","totalVotes":11658,"leadingParty":"KESK","partyData":[{"name":"KESK","votes":7668},{"name":"SDE","votes":2317},{"name":"REF","votes":1026},{"name":"VLÕV","votes":554},{"name":"ROH","votes":48},{"name":"IRL","votes":45}]},{"id":"0338","totalVotes":3332,"leadingParty":"VLMV","partyData":[{"name":"VLMV","votes":1494},{"name":"VLUA","votes":1164},{"name":"SDE","votes":463},{"name":"KESK","votes":145},{"name":"VLS","votes":66}]},{"id":"0353","totalVotes":3159,"leadingParty":"VÜK","partyData":[{"name":"VÜK","votes":1294},{"name":"VÜKV","votes":862},{"name":"EKRE","votes":294},{"name":"VAKV","votes":289},{"name":"REF","votes":194},{"name":"KESK","votes":183},{"name":"üksik","votes":43}]},{"id":"0424","totalVotes":948,"leadingParty":"KESK","partyData":[{"name":"KESK","votes":778},{"name":"IRL","votes":170}]},{"id":"0432","totalVotes":1742,"leadingParty":"USL","partyData":[{"name":"USL","votes":678},{"name":"VLL","votes":527},{"name":"IRL","votes":491},{"name":"KESK","votes":46}]},{"id":"0431","totalVotes":6064,"leadingParty":"VL VLÜ","partyData":[{"name":"VL VLÜ","votes":3220},{"name":"REF","votes":1126},{"name":"VL HT","votes":1051},{"name":"IRL","votes":223},{"name":"VL UV","votes":155},{"name":"KESK","votes":150},{"name":"EKRE","votes":139}]},{"id":"0441","totalVotes":3478,"leadingParty":"VLÜK","partyData":[{"name":"VLÜK","votes":1820},{"name":"SDE","votes":744},{"name":"EKRE","votes":391},{"name":"REF","votes":318},{"name":"KESK","votes":123},{"name":"VLKKN","votes":82}]},{"id":"0430","totalVotes":2702,"leadingParty":"LRVL","partyData":[{"name":"LRVL","votes":1728},{"name":"VLNVÜ","votes":631},{"name":"EKRE","votes":333},{"name":"KESK","votes":10}]},{"id":"0442","totalVotes":4340,"leadingParty":"KESK","partyData":[{"name":"KESK","votes":1202},{"name":"VLMV","votes":842},{"name":"VLMK","votes":801},{"name":"VLOVE","votes":793},{"name":"REF","votes":682},{"name":"üksik","votes":20}]},{"id":"0446","totalVotes":6274,"leadingParty":"KESK","partyData":[{"name":"KESK","votes":3752},{"name":"ESVL","votes":692},{"name":"VLMMK","votes":505},{"name":"EÜVP","votes":474},{"name":"VLMM","votes":423},{"name":"REF","votes":250},{"name":"IRL","votes":117},{"name":"üksik","votes":38},{"name":"VLEE","votes":23}]},{"id":"0478","totalVotes":1034,"leadingParty":"IRL","partyData":[{"name":"IRL","votes":781},{"name":"REF","votes":177},{"name":"EKRE","votes":76}]},{"id":"0480","totalVotes":4133,"leadingParty":"KESK","partyData":[{"name":"KESK","votes":1431},{"name":"IRL","votes":1198},{"name":"REF","votes":575},{"name":"VLÜM","votes":486},{"name":"EKRE","votes":162},{"name":"VLKM","votes":154},{"name":"SDE","votes":127}]},{"id":"0486","totalVotes":3170,"leadingParty":"VLUL","partyData":[{"name":"VLUL","votes":1058},{"name":"SDE","votes":766},{"name":"REF","votes":713},{"name":"KESK","votes":601},{"name":"VLUV","votes":31},{"name":"üksik","votes":1}]},{"id":"0503","totalVotes":3363,"leadingParty":"VLM","partyData":[{"name":"VLM","votes":1289},{"name":"VLMI","votes":838},{"name":"EKRE","votes":601},{"name":"VLV","votes":531},{"name":"KESK","votes":104}]},{"id":"0511","totalVotes":21192,"leadingParty":"KESK","partyData":[{"name":"KESK","votes":14160},{"name":"VL MN","votes":4947},{"name":"ES VL","votes":969},{"name":"VL P","votes":637},{"name":"REF","votes":281},{"name":"IRL","votes":198}]},{"id":"0514","totalVotes":2437,"leadingParty":"ÜHKO","partyData":[{"name":"ÜHKO","votes":1159},{"name":"KESK","votes":990},{"name":"VÜKE","votes":274},{"name":"üksik","votes":14}]},{"id":"0528","totalVotes":1923,"leadingParty":"VN","partyData":[{"name":"VN","votes":875},{"name":"IRL","votes":874},{"name":"KESK","votes":96},{"name":"REF","votes":65},{"name":"üksik","votes":13}]},{"id":"0557","totalVotes":3436,"leadingParty":"VLÜK","partyData":[{"name":"VLÜK","votes":1611},{"name":"REF","votes":1016},{"name":"KESK","votes":522},{"name":"EKRE","votes":146},{"name":"IRL","votes":114},{"name":"VLVV","votes":27}]},{"id":"0567","totalVotes":0,"leadingParty":null,"partyData":[]},{"id":"0586","totalVotes":2984,"leadingParty":"UV","partyData":[{"name":"UV","votes":1657},{"name":"PÜK","votes":634},{"name":"PMK","votes":322},{"name":"KK","votes":234},{"name":"KESK","votes":113},{"name":"üksik","votes":24}]},{"id":"0624","totalVotes":20177,"leadingParty":"REF","partyData":[{"name":"REF","votes":4604},{"name":"KESK","votes":3860},{"name":"IRL","votes":3818},{"name":"VLPÜ","votes":3403},{"name":"EKRE","votes":3116},{"name":"SDE","votes":1096},{"name":"VLMP","votes":219},{"name":"VLEEK","votes":61}]},{"id":"0638","totalVotes":3740,"leadingParty":"","partyData":[{"name":"","votes":1486},{"name":"","votes":1237},{"name":"","votes":700},{"name":"EKRE","votes":157},{"name":"üksik","votes":121},{"name":"KESK","votes":39}]},{"id":"0615","totalVotes":3725,"leadingParty":"VLKT","partyData":[{"name":"VLKT","votes":2174},{"name":"VLKH","votes":849},{"name":"REF","votes":539},{"name":"EKRE","votes":114},{"name":"KESK","votes":49}]},{"id":"0618","totalVotes":4722,"leadingParty":"VLÜP","partyData":[{"name":"VLÜP","votes":1691},{"name":"SDE","votes":1300},{"name":"KESK","votes":559},{"name":"REF","votes":551},{"name":"EKRE","votes":416},{"name":"IRL","votes":205}]},{"id":"0622","totalVotes":6697,"leadingParty":"REF","partyData":[{"name":"REF","votes":1822},{"name":"KESK","votes":1659},{"name":"TPV","votes":1165},{"name":"SDE","votes":742},{"name":"VPA","votes":663},{"name":"EKRE","votes":622},{"name":"üksik","votes":24}]},{"id":"0651","totalVotes":2331,"leadingParty":"VLRV","partyData":[{"name":"VLRV","votes":1017},{"name":"KEVL","votes":809},{"name":"üksik","votes":258},{"name":"VLÜE","votes":191},{"name":"IRL","votes":56}]},{"id":"0653","totalVotes":7162,"leadingParty":"REF","partyData":[{"name":"REF","votes":3758},{"name":"SDE","votes":1254},{"name":"IRL","votes":852},{"name":"EKRE","votes":638},{"name":"KESK","votes":608},{"name":"üksik","votes":52}]},{"id":"0663","totalVotes":6868,"leadingParty":"REF","partyData":[{"name":"REF","votes":2264},{"name":"IRL","votes":1630},{"name":"KESK","votes":862},{"name":"SDE","votes":739},{"name":"EKRE","votes":714},{"name":"VRH","votes":597},{"name":"üksik","votes":62}]},{"id":"0661","totalVotes":2309,"leadingParty":"","partyData":[{"name":"","votes":1397},{"name":"IRL","votes":391},{"name":"SDE","votes":199},{"name":"KESK","votes":194},{"name":"EKRE","votes":128}]},{"id":"0668","totalVotes":5661,"leadingParty":"VL RVL","partyData":[{"name":"VL RVL","votes":1594},{"name":"VL ÜK","votes":1181},{"name":"EKRE","votes":1004},{"name":"REF","votes":925},{"name":"KESK","votes":429},{"name":"IRL","votes":321},{"name":"SDE","votes":169},{"name":"üksik","votes":38}]},{"id":"0689","totalVotes":112,"leadingParty":"VLÜR","partyData":[{"name":"VLÜR","votes":64},{"name":"üksik","votes":48}]},{"id":"0708","totalVotes":3240,"leadingParty":"REF","partyData":[{"name":"REF","votes":1018},{"name":"VLÜV","votes":841},{"name":"SDE","votes":484},{"name":"EKRE","votes":464},{"name":"KESK","votes":433}]},{"id":"0698","totalVotes":2996,"leadingParty":"VL MV","partyData":[{"name":"VL MV","votes":1514},{"name":"IRL","votes":936},{"name":"EKRE","votes":207},{"name":"KESK","votes":197},{"name":"KM VL","votes":142}]},{"id":"0712","totalVotes":2354,"leadingParty":"KDV","partyData":[{"name":"KDV","votes":863},{"name":"US","votes":790},{"name":"KST","votes":388},{"name":"EKRE","votes":193},{"name":"üksik","votes":55},{"name":"OK","votes":45},{"name":"KESK","votes":20}]},{"id":"0714","totalVotes":13159,"leadingParty":"SDE","partyData":[{"name":"SDE","votes":3563},{"name":"REF","votes":3052},{"name":"VLS","votes":2812},{"name":"KESK","votes":1090},{"name":"EKRE","votes":874},{"name":"IRL","votes":532},{"name":"VLMS","votes":498},{"name":"üksik","votes":395},{"name":"VLSÖ","votes":343}]},{"id":"0719","totalVotes":4244,"leadingParty":"VVL","partyData":[{"name":"VVL","votes":1343},{"name":"REF","votes":1098},{"name":"VLK","votes":825},{"name":"VLE","votes":606},{"name":"EKRE","votes":244},{"name":"KESK","votes":86},{"name":"VLMLST","votes":42}]},{"id":"0726","totalVotes":9878,"leadingParty":"VLKMV","partyData":[{"name":"VLKMV","votes":4419},{"name":"REF","votes":1647},{"name":"KESK","votes":968},{"name":"IRL","votes":963},{"name":"EKRE","votes":938},{"name":"VLM","votes":563},{"name":"SDE","votes":351},{"name":"üksik","votes":29}]},{"id":"0732","totalVotes":1936,"leadingParty":"VLÜS","partyData":[{"name":"VLÜS","votes":1500},{"name":"VLINH","votes":253},{"name":"KESK","votes":183}]},{"id":"0735","totalVotes":5727,"leadingParty":"KESK","partyData":[{"name":"KESK","votes":3936},{"name":"SVS","votes":1791}]},{"id":"0792","totalVotes":4362,"leadingParty":"VLVJ","partyData":[{"name":"VLVJ","votes":1616},{"name":"KESK","votes":892},{"name":"REF","votes":786},{"name":"VLKT","votes":760},{"name":"EKRE","votes":172},{"name":"IRL","votes":97},{"name":"üksik","votes":29},{"name":"EÜVP","votes":10}]},{"id":"0796","totalVotes":4471,"leadingParty":"VLKE","partyData":[{"name":"VLKE","votes":2612},{"name":"VLÜV","votes":634},{"name":"SDE","votes":493},{"name":"VLKTV","votes":332},{"name":"REF","votes":172},{"name":"KESK","votes":168},{"name":"üksik","votes":46},{"name":"VLEE","votes":14}]},{"id":"0803","totalVotes":2129,"leadingParty":"VLÜV","partyData":[{"name":"VLÜV","votes":1085},{"name":"SDE","votes":523},{"name":"KESK","votes":235},{"name":"REF","votes":172},{"name":"IRL","votes":114}]},{"id":"0809","totalVotes":4699,"leadingParty":"VLUK","partyData":[{"name":"VLUK","votes":1591},{"name":"VLKV","votes":1182},{"name":"KESK","votes":973},{"name":"VLLP","votes":545},{"name":"EKRE","votes":362},{"name":"üksik","votes":46}]},{"id":"0824","totalVotes":3517,"leadingParty":"REF","partyData":[{"name":"REF","votes":1662},{"name":"SDE","votes":927},{"name":"IRL","votes":380},{"name":"KESK","votes":319},{"name":"EKRE","votes":137},{"name":"VLHK","votes":92}]},{"id":"0834","totalVotes":4948,"leadingParty":"IRL","partyData":[{"name":"IRL","votes":1904},{"name":"SDE","votes":1459},{"name":"REF","votes":637},{"name":"EKRE","votes":422},{"name":"KESK","votes":345},{"name":"SJVL","votes":181}]},{"id":"0855","totalVotes":7024,"leadingParty":"KESK","partyData":[{"name":"KESK","votes":2790},{"name":"REF","votes":1823},{"name":"SDE","votes":1395},{"name":"IRL","votes":434},{"name":"EKRE","votes":375},{"name":"VEO","votes":207}]},{"id":"0890","totalVotes":8896,"leadingParty":"REF","partyData":[{"name":"REF","votes":3367},{"name":"IRL","votes":1057},{"name":"SDE","votes":1045},{"name":"EKRE","votes":932},{"name":"KESK","votes":756},{"name":"VLKV","votes":705},{"name":"VLRR","votes":609},{"name":"TVVL","votes":407},{"name":"EÜVP","votes":18}]},{"id":"0897","totalVotes":7375,"leadingParty":"SDE","partyData":[{"name":"SDE","votes":2366},{"name":"REF","votes":2095},{"name":"IRL","votes":1463},{"name":"EKRE","votes":697},{"name":"KESK","votes":696},{"name":"VKV","votes":58}]},{"id":"0899","totalVotes":5940,"leadingParty":"IRL","partyData":[{"name":"IRL","votes":2692},{"name":"REF","votes":935},{"name":"SDE","votes":887},{"name":"VVK","votes":643},{"name":"EKRE","votes":487},{"name":"KESK","votes":289},{"name":"üksik","votes":7}]},{"id":"0901","totalVotes":3321,"leadingParty":"KESK","partyData":[{"name":"KESK","votes":1344},{"name":"IRL","votes":646},{"name":"VLÜV","votes":498},{"name":"SDE","votes":422},{"name":"EKRE","votes":345},{"name":"REF","votes":66}]},{"id":"0903","totalVotes":2613,"leadingParty":"VLMK","partyData":[{"name":"VLMK","votes":1109},{"name":"VLRV","votes":916},{"name":"KESK","votes":588}]},{"id":"0907","totalVotes":265,"leadingParty":"VTV","partyData":[{"name":"VTV","votes":247},{"name":"KESK","votes":18}]},{"id":"0928","totalVotes":2935,"leadingParty":"REF","partyData":[{"name":"REF","votes":958},{"name":"MEIE PANDIVERE","votes":637},{"name":"KESK","votes":433},{"name":"Koduvald","votes":385},{"name":"IRL","votes":208},{"name":"Terve vald","votes":204},{"name":"EKRE","votes":86},{"name":"üksik","votes":24}]},{"id":"0919","totalVotes":5722,"leadingParty":"SDE","partyData":[{"name":"SDE","votes":2458},{"name":"KESK","votes":783},{"name":"REF","votes":769},{"name":"IRL","votes":695},{"name":"EKRE","votes":435},{"name":"VLV","votes":269},{"name":"VLKENE","votes":265},{"name":"VLVEE","votes":48}]},{"id":"0917","totalVotes":4947,"leadingParty":"VLÜVE","partyData":[{"name":"VLÜVE","votes":3705},{"name":"REF","votes":564},{"name":"IRL","votes":368},{"name":"KESK","votes":310}]}];

rawData.forEach((item) => {
    item.fill = partyColor(item.leadingParty)
    item.stroke = "#282c34 0.2"
});

let data = anychart.data.set(rawData);

let map = anychart.choropleth(data);
map.geoData(localMap);
map.background().fill("#282c34");
map.interactivity().selectionMode("none");
map.tooltip().format(function(e){
    let totalVotes = e.getData("totalVotes");
    let result =  "Votes counted: " + totalVotes +"\n";
    e.getData("partyData").forEach((party) =>{
        result += party.name + " - " + party.votes + " (" + (party.votes / totalVotes * 100).toFixed(2) + "%)\n"
    });
    return result;
});

class App extends Component {

    render() {
        return (
            <div className="App">
                <span>
                    <h2>Eesti KOV 2021 valimisõhtu</h2>
                </span>
                <AnyChart
                    instance={map}
                />

            </div>
        );
    }
}

export default App;
