import axios from 'axios'

/**
 * 滑块验证码接口(裸 axios)
 *
 * 后端 CaptchaController 返回 aJ-Captcha 的 ResponseModel 结构({ repCode, repMsg, repData }),
 * 与 CommonResult 不同,不能走 request 拦截器。POST 到 /admin-api 前缀,无 token 也可访问。
 */
export async function getCode(data: any): Promise<any> {
  const res = await axios.post('/admin-api/system/captcha/get', data)
  return res.data
}

export async function reqCheck(data: any): Promise<any> {
  const res = await axios.post('/admin-api/system/captcha/check', data)
  return res.data
}
