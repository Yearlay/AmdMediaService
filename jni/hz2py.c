#include "py.h"
#include "hz2py.h"
#include <string.h>

#define HZ2PY_UTF8_CHECK_LENGTH 20
#define HZ2PY_FILE_READ_BUF_ARRAY_SIZE 1000
#define HZ2PY_INPUT_BUF_ARRAY_SIZE 1024
#define HZ2PY_OUTPUT_BUF_ARRAY_SIZE 2048

#define HZ2PY_STR_COPY(to, from, count) \
    ok = 1;\
    i = 0;\
    _tmp = from;\
    while(i < count)\
    {\
        if (*_tmp == '\0')\
        {\
            ok = 0;\
            break;\
        }\
        _tmp ++;\
        i ++;\
    }\
    if (ok)\
{\
    i = 0;\
    while(i < count)\
    {\
        *to = *from;\
        to ++;\
        from ++;\
        i ++;\
    }\
}\
else\
{\
    if (overage_buff != NULL)\
    {\
        while(*from != '\0')\
        {\
            *overage_buff = *from;\
            from ++;\
        }\
    }\
    break;\
}

//将utf8编码的字符串中的汉字解成拼音
// in 输入
// out 输出
// first_letter_only 是否只输出拼音首字母
// polyphone_support 是否输出多音字
// add_blank 是否在拼音之间追加空格
// convert_double_char 是否转换全角字符为半角字符
// overage_buff 末尾如果有多余的不能组成完整utf8字符的字节，将写到overage_buff，传NULL将输出到out
void utf8_to_pinyin(char *in, char *out, int first_letter_only,
        int polyphone_support, int add_blank, int convert_double_char,
        char *overage_buff) {
    int i = 0;
    char *utf = in;
    char *_tmp;
    char *_tmp2;
    char py_tmp[30] = "";
    char py_tmp2[30] = "";
    char *out_start_flag = out;
    int uni;
    int ok = 0;
    while (*utf != '\0') {
        if ((*utf >> 7) == 0) {
            HZ2PY_STR_COPY(out, utf, 1);
            //如果为一个字节加上#号分隔
            // *out = '#'; //用#号做为分隔符
            // out++;
            //去掉其它的英文只留汉字
            //只能搜索到汉字拼音里面字母
            //          out--;
            //          *out = ' ';
        }
        //两个字节
        else if ((*utf & 0xE0) == 0xC0) {
            HZ2PY_STR_COPY(out, utf, 2);
        }
        //三个字节
        else if ((*utf & 0xF0) == 0xE0) {
            if (*(utf + 1) != '\0' && *(utf + 2) != '\0') {
                uni = (((int) (*utf & 0x0F)) << 12)
                        | (((int) (*(utf + 1) & 0x3F)) << 6)
                        | (*(utf + 2) & 0x3F);

                if (uni > 19967 && uni < 40870) {
                    memset(py_tmp, '\0', 30);
                    memset(py_tmp2, '\0', 30);
                    strcpy(py_tmp, _pinyin_table_[uni - 19968]);
                    _tmp = py_tmp;
                    _tmp2 = py_tmp2;

                    if (first_letter_only == 1) {
                        *_tmp2 = *_tmp;
                        _tmp++;
                        _tmp2++;
                        while (*_tmp != '\0') {
                            if (*_tmp == '|' || *(_tmp - 1) == '|') {
                                *_tmp2 = *_tmp;
                                _tmp2++;
                            }
                            _tmp++;
                        }
                    } else {
                        strcpy(py_tmp2, py_tmp);
                    }

                    _tmp2 = py_tmp2;

                    if (polyphone_support == 0) {
                        while (*_tmp2 != '\0') {
                            if (*_tmp2 == ',') {
                                *_tmp2 = '\0';
                                break;
                            }
                            _tmp2++;
                        }
                        _tmp2 = py_tmp2;
                    }
                    if (add_blank && first_letter_only == 0 && *(out - 1) != '#') {
                        *out = '#'; //用#号做为分隔符
                        out++;
                    }
                    strcpy(out, _tmp2);
                    out += strlen(_tmp2);
                    if (add_blank && first_letter_only == 0) {
                        *out = '#'; //用#号做为分隔符
                        out++;
                    }
                    utf += 3;
                } else if (convert_double_char && uni > 65280 && uni < 65375) {
                    *out = uni - 65248;
                    out++;
                    utf += 3;
                } else if (convert_double_char && uni == 12288) {
                    *out = 32;
                    out++;
                    utf += 3;
                } else {
                    HZ2PY_STR_COPY(out, utf, 3);
                }
            } else {
                HZ2PY_STR_COPY(out, utf, 3);
            }
        }
        //四个字节
        else if ((*utf & 0xF8) == 0xF0) {
            HZ2PY_STR_COPY(out, utf, 4);
        }
        //五个字节
        else if ((*utf & 0xFC) == 0xF8) {
            HZ2PY_STR_COPY(out, utf, 5);
        }
        //六个字节
        else if ((*utf & 0xFE) == 0xFC) {
            HZ2PY_STR_COPY(out, utf, 6);
        } else {
            if (overage_buff != NULL) {
                *overage_buff = *utf;
                overage_buff++;
            } else {
                HZ2PY_STR_COPY(out, utf, 1);
            }
            break;
        }
    }
}

//判断一个字符串是否为utf8编码
int is_utf8_string(char *utf) {
    int length = strlen(utf);
    int check_sub = 0;
    int i = 0;

    if (length > HZ2PY_UTF8_CHECK_LENGTH) {
        length = HZ2PY_UTF8_CHECK_LENGTH;
    }

    for (; i < length; i++) {
        if (check_sub == 0) {
            if ((utf[i] >> 7) == 0) {
                continue;
            } else if ((utf[i] & 0xE0) == 0xC0) {
                check_sub = 1;
            } else if ((utf[i] & 0xF0) == 0xE0) {
                check_sub = 2;
            } else if ((utf[i] & 0xF8) == 0xF0) {
                check_sub = 3;
            } else if ((utf[i] & 0xFC) == 0xF8) {
                check_sub = 4;
            } else if ((utf[i] & 0xFE) == 0xFC) {
                check_sub = 5;
            } else {
                return 0;
            }
        } else {
            if ((utf[i] & 0xC0) != 0x80) {
                return 0;
            }
            check_sub--;
        }
    }
    return 1;
}

// first_letter_only 是否只输出拼音首字母
int hztpy(const char *read_buff, char *firstPYBuf, int first_letter_only) {
    char overage_buff[7] = { 0 };
    char *_tmp = NULL;
    char inbuf[HZ2PY_INPUT_BUF_ARRAY_SIZE] = { 0 };
    int add_blank = 0; // 当前不用考虑添加分割父，如果考虑，需要修改该值为1.
    int polyphone_support = 0; // 当前不考虑多音字，如果考虑需要该值为1.
    int convert_double_char = 0;

    // convert_double_char 是否转换全角字符为半角字符
    // overage_buff 末尾如果有多余的不能组成完整utf8字符的字节，将写到overage_buff，传NULL将输出到out

    _tmp = inbuf;
    if (strlen(overage_buff)) {
        strcpy(_tmp, overage_buff);
        _tmp += strlen(overage_buff);
        memset(overage_buff, '\0', 7);
    }
    strcpy(_tmp, read_buff);
    if (!is_utf8_string(inbuf)) {
        return -1;
    }
    utf8_to_pinyin(inbuf, firstPYBuf, first_letter_only, polyphone_support,
            add_blank, convert_double_char, overage_buff);
    return 1;
}
