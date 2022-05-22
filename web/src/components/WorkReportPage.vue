<template>
    <div class="content-container">
        <div class="input-container">
            <p> 今日工作总结 </p>
            <textarea placeholder="请输入汇报内容" v-model="report.content"/>
        </div>
        <Divider/>
        <div class="input-container">
            <p> 明日计划 </p>
            <textarea placeholder="请输入汇报内容" v-model="report.tomorrowPlan"/>
        </div>
        <Divider/>
        <div class="input-container">
            <p> 需要帮助与协调 </p>
            <textarea placeholder="请输入汇报内容" v-model="report.requirement"/>
        </div>
        <Divider/>
        <div class="option-item" @click="chooseContacts">
            <p>{{ `发送到人${userTargetDesc}` }}</p>
            <p>&gt;</p>
        </div>
        <Divider/>
        <div class="option-item">
            <p>{{ `发送到群${groupTargetDesc}` }}</p>
            <p>&gt;</p>
        </div>
        <Divider/>
        <div class="action-container">
            <button ref="submitButton" @click="submitReport">提交</button>
        </div>
    </div>
</template>

<script>
import Divider from "@/components/Divider";
import wf from "@/jssdk/wf";
import Report from "@/model/report";
import api from "@/api/api";

export default {
    name: 'WorkReportPage',
    components: {Divider},
    props: {
        account: {
            type: String,
            required: true,
        }
    },
    data() {
        return {
            userTargets: [],
            groupTargets: [],
            report: new Report(this.account)
        }
    },
    methods: {
        chooseContacts() {
            wf.biz.chooseContacts({max: 1}, (userInfos) => {
                this.userTargets = userInfos;
                this.report.reportTo = [];
                userInfos.forEach(u => {
                    this.report.reportTo.push(u.uid);
                })
            }, (err) => {
                console.log('chooseContacts failed', err);
            })
        },

        submitReport() {
            if (!this.report.content || !this.report.content.trim()) {
                wf.toast('汇报内容不能为空');
                return;
            }

            if (!this.report.reportTo || this.report.reportTo.length === 0) {
                wf.toast('请选择汇报对象')
                return;
            }

            this.report.title = '日报';

            api.report(this.report).then(() => {
                // TODO 切换到一个成功页面
                this.$refs.submitButton.textContent = '提交成功';
                this.$refs.submitButton.disabled = true;
                wf.toast('日报提交成功')
            }).catch(reason => {
                wf.toast('日报提交失败 ' + reason);
            })
        }
    },
    computed: {
        userTargetDesc() {
            if (this.userTargets.length > 0) {
                let desc = ' ';
                this.userTargets.forEach(u => {
                    desc += u.displayName;
                    desc += ' ';
                })
                return desc;
            } else {
                return '';
            }
        },
        groupTargetDesc() {
            if (this.groupTargets.length > 0) {
                let desc = '';
                this.groupTargets.forEach(g => {
                    desc += g.name;
                    desc += ' ';
                })
                return desc;
            } else {
                return '';
            }
        }
    }
}
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>

.content-container {
    position: relative;
    padding-top: 10px;
    height: 100%;
}

.input-container {
    padding: 0 10px;
}

p {
    text-align: left;
    font-size: 14px;
}

textarea {
    width: 100%;
    max-height: 200px;
    height: 60px;
    border: none;
    background-color: transparent;
    resize: none;
    outline: none;
    padding-bottom: 10px;
    font-size: 16px;

}

textarea:active {
    border: none;
}

.option-item {
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
    padding: 0 10px;
    height: 60px;
}

.option-item:active {
    background-color: lightgrey;
}

.action-container {
    margin-top: 60px;
}

.action-container button {
    width: 200px;
    height: 40px;
}


</style>
