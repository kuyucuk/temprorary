/**
* Version: 1.0
*/
package com.smartdevice.aidl;

interface ICallBack
{
	/**
	 *Receive callback message
	 */
	void onReturnValue(in byte[] data, int size);
}
