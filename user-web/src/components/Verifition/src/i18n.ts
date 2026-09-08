/**
 * 验证码组件文案(user-web 未集成 vue-i18n,直接提供中文文案)
 */
const messages: Record<string, string> = {
  'captcha.code': '验证码',
  'captcha.fail': '验证失败,请重试',
  'captcha.point': '点击上方文字',
  'captcha.slide': '请按住滑块拖动',
  'captcha.success': '验证成功',
  'captcha.verification': '温馨提示',
  'captcha.verify': '拖动滑块完成验证'
}

export function t(key: string): string {
  return messages[key] || key
}
