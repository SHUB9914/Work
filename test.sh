awk -f lc.awk $(find api/src/main -name "*.scala") 2>&1 | sed -r 's/'$(echo -e "\033")'\[[0-9]{1,2}(;([0-9]{1,2})?)?[mK]//g' | tee api/target/stats.log

awk -f lc.awk $(find common/src/main -name "*.scala") 2>&1 | sed -r 's/'$(echo -e "\033")'\[[0-9]{1,2}(;([0-9]{1,2})?)?[mK]//g' | tee common/target/stats.log

awk -f lc.awk $(find persistence/src/main -name "*.scala") 2>&1 | sed -r 's/'$(echo -e "\033")'\[[0-9]{1,2}(;([0-9]{1,2})?)?[mK]//g' | tee persistence/target/stats.log

awk -f lc.awk $(find spok/src/main -name "*.scala") 2>&1 | sed -r 's/'$(echo -e "\033")'\[[0-9]{1,2}(;([0-9]{1,2})?)?[mK]//g' | tee spok/target/stats.log

awk -f lc.awk $(find notification/src/main -name "*.scala") 2>&1 | sed -r 's/'$(echo -e "\033")'\[[0-9]{1,2}(;([0-9]{1,2})?)?[mK]//g' | tee notification/target/stats.log


